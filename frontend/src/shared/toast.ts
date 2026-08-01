export type ToastVariant = "error" | "success" | "warning" | "neutral";

export type ToastMessage = {
  id: number;
  text: string;
  variant: ToastVariant;
};
