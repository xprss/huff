import { timingSafeEqual } from 'node:crypto';
import { MastraAuthProvider } from '@mastra/core/server';

type InternalUser = { readonly id: 'hexaquot-backend' };

class InternalBackendAuth extends MastraAuthProvider<InternalUser> {
  constructor() {
    super({ name: 'hexaquot-backend' });
    this.registerOptions({
      protected: ['/api/*', '/internal/*'],
      public: [['/internal/health', 'GET']],
    });
  }

  async authenticateToken(token: string): Promise<InternalUser | null> {
    const expected = process.env.GAME_STATS_AGENT_TOKEN;
    if (!expected) return null;

    const suppliedBytes = Buffer.from(token);
    const expectedBytes = Buffer.from(expected);
    if (suppliedBytes.length !== expectedBytes.length) return null;
    return timingSafeEqual(suppliedBytes, expectedBytes)
      ? { id: 'hexaquot-backend' }
      : null;
  }

  async authorizeUser(user: InternalUser): Promise<boolean> {
    return user.id === 'hexaquot-backend';
  }
}

export const internalAuth = new InternalBackendAuth();
