import { Skeleton } from "@/components/ui/skeleton";

export default function PosQuickItemsLoading() {
  return (
    <div className="flex flex-col gap-4 p-4 sm:p-6 lg:p-8">
      <Skeleton className="h-8 w-44" />
      <Skeleton className="h-12 w-full rounded-xl" />
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
        {Array.from({ length: 8 }).map((_, i) => (
          <Skeleton key={i} className="h-24 w-full rounded-lg" />
        ))}
      </div>
    </div>
  );
}
