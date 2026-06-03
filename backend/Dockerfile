# ── Stage 1: Builder ──────────────────────────────────────────────────────────
# Install all dependencies (including devDeps) and compile TypeScript.
FROM node:22-alpine AS builder
WORKDIR /app

# Copy manifests first. Docker layer-caches the npm install step and only
# re-runs it when package.json or package-lock.json actually changes —
# not on every source file edit.
COPY package.json package-lock.json ./
RUN npm ci

# Compile TypeScript → dist/
COPY tsconfig.json ./
COPY src/ ./src/
RUN npm run build

# ── Stage 2: Runner ───────────────────────────────────────────────────────────
# Lean production image: only the compiled output and production dependencies.
# No TypeScript compiler, no tsx, no source files, no .env.
FROM node:22-alpine AS runner
WORKDIR /app

# Create a non-root user. Cloud Run containers run as root by default;
# dropping privileges is a standard security hardening step.
RUN addgroup -S lynx && adduser -S lynx -G lynx

# Reinstall only production dependencies from the lockfile.
# We do NOT copy node_modules from the builder — the builder's node_modules
# contains devDependencies (tsc, tsx, @types/*). A clean --omit=dev install
# gives us a smaller, correct set.
COPY package.json package-lock.json ./
RUN npm ci --omit=dev

# Pull the compiled output from the builder stage.
COPY --from=builder /app/dist ./dist

USER lynx

# Document the port the app binds to (Cloud Run reads the PORT env var,
# which defaults to 8080 in our index.ts).
EXPOSE 8080

# Run node directly — NOT via npm start. This ensures Cloud Run's SIGTERM
# reaches the Node process (which has the graceful shutdown handler) rather
# than being swallowed by an npm wrapper process.
CMD ["node", "dist/index.js"]
