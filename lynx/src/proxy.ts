import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";
import { auth0 } from "@/lib/auth0";

export async function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;

  // Let Auth0's own routes through without an auth check
  if (pathname.startsWith("/auth")) {
    return await auth0.middleware(request);
  }

  // For everything else, require a session
  const session = await auth0.getSession(request);
  if (!session) {
    const loginUrl = new URL("/auth/login", request.url);
    loginUrl.searchParams.set("returnTo", pathname);
    return NextResponse.redirect(loginUrl);
  }

  return await auth0.middleware(request);
}

export const config = {
  matcher: ["/((?!_next/static|_next/image|favicon.ico).*)"],
};
