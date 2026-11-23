import {
  Anchor, Bike, BookOpen, Briefcase, Camera, ChefHat, Clock, Compass, Crown, Feather, FlaskConical, Gem, Glasses, Hammer,
  Key, Lamp, Map, Moon, Music, Palette, ScrollText, Shield, Ship, Star, Stethoscope, Sun, Telescope, TrainFront, TreePine,
  UserRound, Wrench, type LucideIcon
} from "lucide-react";

/** Whitelist shared with the backend validator: the agent can only pick these names. */
const ICONS: Record<string, LucideIcon> = {
  anchor: Anchor, feather: Feather, crown: Crown, key: Key, compass: Compass, lamp: Lamp, book: BookOpen, hammer: Hammer,
  palette: Palette, music: Music, flask: FlaskConical, ship: Ship, train: TrainFront, tree: TreePine, camera: Camera,
  scroll: ScrollText, shield: Shield, star: Star, moon: Moon, sun: Sun, glasses: Glasses, briefcase: Briefcase, bike: Bike,
  chef: ChefHat, telescope: Telescope, wrench: Wrench, stethoscope: Stethoscope, gem: Gem, map: Map, clock: Clock
};

export function characterIcon(name: string): LucideIcon {
  return ICONS[name] ?? UserRound;
}
