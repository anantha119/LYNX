import { createRemoteJWKSet, jwtVerify } from "jose";
import type { MiddlewareHandler } from "hono";
import { getOrCreateUser } from "../db/users.js";

const jwks = createRemoteJWKSet(
  new URL(`https://${process.env.AUTH0_DOMAIN}/.well-known/jwks.json`)
);

export const authMiddleware: MiddlewareHandler = async (c, next) => {
  const authHeader = c.req.header("Authorization");
  if (!authHeader?.startsWith("Bearer ")) {
    return c.json({ error: "Unauthorized" }, 401);
  }
  const token = authHeader.slice(7);

  try {
    const { payload } = await jwtVerify(token, jwks, {
      audience: process.env.AUTH0_AUDIENCE,
      issuer: `https://${process.env.AUTH0_DOMAIN}/`,
    });

    const auth0Sub = payload.sub!;
    const userId = await getOrCreateUser(auth0Sub, token);
    c.set("userId", userId);

    await next();
  } catch (err) {
    console.error("[auth] JWT verification failed:", err);
    return c.json({ error: "Unauthorized" }, 401);
  }
};
