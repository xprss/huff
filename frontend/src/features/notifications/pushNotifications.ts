import { api } from "../../api";
import type { PushSubscriptionDto } from "../../types";

const GAME_NOTIFICATIONS_ENABLED_KEY = "huffGameNotificationsEnabled";
const PUSH_PUBLIC_KEY_KEY = "huffPushPublicKey";
const SERVICE_WORKER_READY_TIMEOUT_MS = 2500;

export function getNotificationPermission(): NotificationPermission {
  return "Notification" in window ? Notification.permission : "default";
}

export function areGameNotificationsEnabled() {
  return localStorage.getItem(GAME_NOTIFICATIONS_ENABLED_KEY) === "true" && getNotificationPermission() === "granted";
}

export function setGameNotificationsEnabled(enabled: boolean) {
  if (enabled) {
    localStorage.setItem(GAME_NOTIFICATIONS_ENABLED_KEY, "true");
  } else {
    localStorage.removeItem(GAME_NOTIFICATIONS_ENABLED_KEY);
    localStorage.removeItem(PUSH_PUBLIC_KEY_KEY);
  }
}

export function getNotificationMenuLabel(enabled: boolean, permission: NotificationPermission) {
  if (enabled) return "Disattiva notifiche";
  if (permission === "denied") return "Notifiche bloccate";
  return "Attiva notifiche";
}

export async function enableGameNotifications() {
  if (!arePushNotificationsSupported()) {
    throw new Error("Le notifiche push non sono supportate da questo browser.");
  }

  const settings = await api.pushSettings();
  if (!settings.supported || !settings.publicKey) {
    throw new Error("Notifiche push non configurate sul server.");
  }

  let permission = getNotificationPermission();
  if (permission === "default") {
    permission = await Notification.requestPermission();
  }
  if (permission !== "granted") {
    throw new Error(
      permission === "denied"
        ? "Notifiche bloccate: abilitarle dalle impostazioni del browser."
        : "Notifiche non attivate."
    );
  }

  await subscribeToGamePush(settings.publicKey);
  setGameNotificationsEnabled(true);
}

export async function disableGameNotifications() {
  setGameNotificationsEnabled(false);

  if (!arePushNotificationsSupported()) return;

  const registration = await getReadyServiceWorkerRegistration();
  const subscription = await registration.pushManager.getSubscription();
  if (!subscription) return;

  await api.deletePushSubscription(toPushSubscriptionDto(subscription)).catch(() => undefined);
  await subscription.unsubscribe();
}

export async function syncPushSubscription() {
  if (getNotificationPermission() !== "granted") {
    throw new Error("Notifiche non autorizzate.");
  }

  const settings = await api.pushSettings();
  if (!settings.supported || !settings.publicKey) {
    throw new Error("Notifiche push non configurate sul server.");
  }

  await subscribeToGamePush(settings.publicKey);
}

function arePushNotificationsSupported() {
  return "Notification" in window && "serviceWorker" in navigator && "PushManager" in window;
}

async function subscribeToGamePush(publicKey: string) {
  const registration = await getReadyServiceWorkerRegistration();
  let existingSubscription = await registration.pushManager.getSubscription();
  if (existingSubscription && localStorage.getItem(PUSH_PUBLIC_KEY_KEY) !== publicKey) {
    await api.deletePushSubscription(toPushSubscriptionDto(existingSubscription)).catch(() => undefined);
    await existingSubscription.unsubscribe();
    existingSubscription = null;
  }
  const subscription =
    existingSubscription ??
    (await registration.pushManager.subscribe({
      userVisibleOnly: true,
      applicationServerKey: urlBase64ToUint8Array(publicKey)
    }));

  await api.savePushSubscription(toPushSubscriptionDto(subscription));
  localStorage.setItem(PUSH_PUBLIC_KEY_KEY, publicKey);
}

async function getReadyServiceWorkerRegistration() {
  await navigator.serviceWorker.register("/sw.js").then((registration) => registration.update());
  return await withTimeout(
    navigator.serviceWorker.ready,
    SERVICE_WORKER_READY_TIMEOUT_MS,
    "Service worker non pronto per le notifiche."
  );
}

function toPushSubscriptionDto(subscription: PushSubscription): PushSubscriptionDto {
  const p256dh = subscription.getKey("p256dh");
  const auth = subscription.getKey("auth");
  if (!p256dh || !auth) {
    throw new Error("Subscription push non valida.");
  }

  return {
    endpoint: subscription.endpoint,
    keys: {
      p256dh: arrayBufferToBase64Url(p256dh),
      auth: arrayBufferToBase64Url(auth)
    }
  };
}

function urlBase64ToUint8Array(value: string) {
  const padding = "=".repeat((4 - (value.length % 4)) % 4);
  const base64 = (value + padding).replace(/-/g, "+").replace(/_/g, "/");
  const rawData = window.atob(base64);
  return Uint8Array.from(rawData, (character) => character.charCodeAt(0));
}

function arrayBufferToBase64Url(buffer: ArrayBuffer) {
  const bytes = new Uint8Array(buffer);
  let binary = "";
  bytes.forEach((byte) => {
    binary += String.fromCharCode(byte);
  });
  return window.btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");
}

function withTimeout<T>(promise: Promise<T>, timeoutMs: number, message: string) {
  return new Promise<T>((resolve, reject) => {
    const timer = window.setTimeout(() => reject(new Error(message)), timeoutMs);
    promise.then(
      (value) => {
        window.clearTimeout(timer);
        resolve(value);
      },
      (error) => {
        window.clearTimeout(timer);
        reject(error);
      }
    );
  });
}
