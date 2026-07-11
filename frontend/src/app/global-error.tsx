"use client";

/**
 * Root-level error boundary. Unlike `error.tsx`, this replaces the entire
 * document (including <html> and <body>) when the root layout itself throws.
 * It must render its own <html> and <body>.
 */
export default function GlobalError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <html lang="en">
      <body
        style={{
          fontFamily: "system-ui, sans-serif",
          display: "flex",
          minHeight: "100vh",
          alignItems: "center",
          justifyContent: "center",
          margin: 0,
          background: "#0a0a0a",
          color: "#f8fafc",
        }}
      >
        <div style={{ textAlign: "center", maxWidth: 420, padding: 24 }}>
          <h1 style={{ fontSize: 24, fontWeight: 600 }}>Application error</h1>
          <p style={{ color: "#94a3b8", marginTop: 12 }}>
            A critical error occurred while loading the application.
          </p>
          {error.digest ? (
            <p style={{ color: "#64748b", fontSize: 12, marginTop: 8 }}>
              Reference: {error.digest}
            </p>
          ) : null}
          <button
            type="button"
            onClick={reset}
            style={{
              marginTop: 24,
              padding: "10px 20px",
              borderRadius: 6,
              border: "none",
              background: "#3b82f6",
              color: "#0a0a0a",
              fontWeight: 600,
              cursor: "pointer",
            }}
          >
            Reload
          </button>
        </div>
      </body>
    </html>
  );
}
