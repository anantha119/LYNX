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

  let payload;
  try {
    const result = await jwtVerify(token, jwks, {
      audience: process.env.AUTH0_AUDIENCE,
      issuer: `https://${process.env.AUTH0_DOMAIN}/`,
    });
    payload = result.payload;
  } catch (err) {
    console.error("[auth] JWT verification failed:", err);
    return c.json({ error: "Unauthorized" }, 401);
  }

  const auth0Sub = payload.sub!;
  let userId: string;
  try {
    userId = await getOrCreateUser(auth0Sub, token);
  } catch (err) {
    console.error("[auth] DB user fetch failed:", err);
    return c.json({ error: "Internal Server Error" }, 500);
  }

  c.set("userId", userId);
  await next();
};
