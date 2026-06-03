# Lynx

A conversational chat interface built with Next.js, TypeScript, and Tailwind CSS. Features a persistent sidebar with conversation history, streaming message rendering, and a dark/light mode toggle.

## Tech Stack

- **Framework**: Next.js (App Router)
- **Language**: TypeScript
- **Styling**: Tailwind CSS v4
- **Components**: shadcn/ui
- **Icons**: Lucide React
- **Fonts**: Syne (display), JetBrains Mono (mono)

## Project Structure

```
src/
├── app/
│   ├── layout.tsx        # Root layout, font setup, metadata
│   ├── page.tsx          # Entry point
│   └── globals.css       # Global styles, CSS variables, theme
└── components/
    └── ui/
        ├── chat-app.tsx       # Top-level orchestrator, state management
        ├── chat-sidebar.tsx   # Conversation list grouped by date
        ├── chat-messages.tsx  # Message thread with streaming support
        ├── v0-ai-chat.tsx     # Chat input with action chips
        └── textarea.tsx       # shadcn Textarea primitive
```

## Getting Started

Install dependencies:

```bash
npm install
```

Run the development server:

```bash
npm run dev
```

Open [http://localhost:3000](http://localhost:3000).

## Dev Server

| Command | Description |
|---------|-------------|
| `npm run dev` | Start the development server |
| `Ctrl+C` | Stop the server (preferred — clean shutdown) |
| `npm run stop` | Force kill if you forgot to `Ctrl+C` |

Open [http://localhost:3000](http://localhost:3000) after starting.

## Build

```bash
npm run build
npm start
```

```bash
Ctrl+C
npm run stop
```

## Lint

```bash
npm run lint
```
