import { Skeleton } from "@/components/ui/skeleton";

export default function GoodsReceiptsLoading() {
  return (
    <div className="flex flex-col gap-4 p-4 sm:p-6 lg:p-8">
      <Skeleton className="h-8 w-48" />
      <Skeleton className="h-12 w-full rounded-xl" />
      <div className="rounded-xl border">
        <div className="border-b p-3">
          <Skeleton className="h-4 w-full" />
        </div>
        {Array.from({ length: 6 }).map((_, i) => (
          <div key={i} className="flex gap-4 border-b p-3 last:border-b-0">
            {Array.from({ length: 8 }).map((__, j) => (
              <Skeleton key={j} className="h-4 flex-1" />
            ))}
          </div>
        ))}
      </div>
    </div>
  );
}
