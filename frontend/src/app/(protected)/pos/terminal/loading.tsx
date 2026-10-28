import { Skeleton } from "@/components/ui/skeleton";

export default function PosTerminalLoading() {
  return (
    <div className="flex h-[calc(100dvh-4rem)] flex-col gap-4 p-4 lg:flex-row">
      <div className="flex-1 space-y-4">
        <Skeleton className="h-16 w-full rounded-lg" />
        <Skeleton className="h-12 w-full rounded-md" />
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
          {Array.from({ length: 8 }).map((_, i) => (
            <Skeleton key={i} className="h-24 w-full rounded-lg" />
          ))}
        </div>
      </div>
      <div className="hidden w-80 space-y-3 lg:block">
        <Skeleton className="h-8 w-24" />
        {Array.from({ length: 4 }).map((_, i) => (
          <Skeleton key={i} className="h-16 w-full rounded-lg" />
        ))}
        <Skeleton className="h-12 w-full rounded-md" />
      </div>
    </div>
  );
}
