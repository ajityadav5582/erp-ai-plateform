"use client";

import { useRef, useState } from "react";
import {
  AlertCircle,
  AlertTriangle,
  CheckCircle2,
  Download,
  FileSpreadsheet,
  FileUp,
  Loader2,
  Upload,
  X,
} from "lucide-react";
import { toast } from "sonner";
import {
  downloadItemTemplateCsv,
  useImportItemsCsvMutation,
  type ItemImportResult,
} from "@/services/item.service";
import { getApiErrorMessage } from "@/services/error-handler";

import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";

interface ImportItemsDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSuccess: () => void;
}

export function ImportItemsDialog({
  open,
  onOpenChange,
  onSuccess,
}: ImportItemsDialogProps) {
  const [file, setFile] = useState<File | null>(null);
  const [isDownloadingTemplate, setIsDownloadingTemplate] = useState(false);
  const [result, setResult] = useState<ItemImportResult | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [importItemsCsv, { isLoading: isUploading }] = useImportItemsCsvMutation();

  const handleReset = () => {
    setFile(null);
    setResult(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = "";
    }
  };

  const handleOpenChange = (nextOpen: boolean) => {
    if (!nextOpen) {
      handleReset();
    }
    onOpenChange(nextOpen);
  };

  const handleDownloadTemplate = async () => {
    try {
      setIsDownloadingTemplate(true);
      await downloadItemTemplateCsv();
      toast.success("CSV template downloaded successfully");
    } catch (err) {
      toast.error(getApiErrorMessage(err, "Failed to download CSV template"));
    } finally {
      setIsDownloadingTemplate(false);
    }
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selected = e.target.files?.[0];
    if (selected) {
      if (!selected.name.toLowerCase().endsWith(".csv")) {
        toast.error("Please select a valid CSV (.csv) file");
        return;
      }
      setFile(selected);
      setResult(null);
    }
  };

  const handleUpload = async () => {
    if (!file) {
      toast.error("Please choose a CSV file to upload");
      return;
    }

    const formData = new FormData();
    formData.append("file", file);

    try {
      const res = await importItemsCsv(formData).unwrap();
      setResult(res);

      if (res.importedCount > 0) {
        onSuccess();
      }

      if (res.failedCount === 0) {
        toast.success(`Successfully imported all ${res.importedCount} items!`);
      } else if (res.importedCount > 0) {
        toast.warning(
          `Imported ${res.importedCount} items, but ${res.failedCount} rows failed.`
        );
      } else {
        toast.error(`Import failed: ${res.failedCount} rows had errors.`);
      }
    } catch (err) {
      toast.error(getApiErrorMessage(err, "Failed to import items CSV"));
    }
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="max-w-2xl max-h-[90vh] flex flex-col overflow-hidden">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <FileSpreadsheet className="size-5 text-emerald-600" />
            Bulk Import Items via CSV
          </DialogTitle>
          <DialogDescription>
            Upload a CSV file to import multiple inventory items, prices, tax classifications,
            and initial opening stock balances in bulk.
          </DialogDescription>
        </DialogHeader>

        <div className="flex-1 overflow-y-auto space-y-4 py-2 pr-1">
          {/* Step 1: Download Template */}
          <div className="flex items-center justify-between rounded-lg border border-border/80 bg-muted/40 p-3">
            <div className="text-xs">
              <span className="font-semibold block text-foreground">
                Need the standard template?
              </span>
              <span className="text-muted-foreground">
                Download the pre-formatted CSV template with sample data for Hardware & Sanitary items.
              </span>
            </div>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={handleDownloadTemplate}
              disabled={isDownloadingTemplate}
              className="gap-1.5 shrink-0"
            >
              {isDownloadingTemplate ? (
                <Loader2 className="size-3.5 animate-spin" />
              ) : (
                <Download className="size-3.5" />
              )}
              Download Template
            </Button>
          </div>

          {/* Step 2: Upload Zone */}
          {!result && (
            <div className="space-y-3">
              <div
                onClick={() => fileInputRef.current?.click()}
                className="flex flex-col items-center justify-center rounded-xl border-2 border-dashed border-border/80 p-8 text-center cursor-pointer hover:border-primary/60 hover:bg-muted/20 transition-colors"
              >
                <input
                  ref={fileInputRef}
                  type="file"
                  accept=".csv"
                  onChange={handleFileChange}
                  className="hidden"
                />
                <FileUp className="size-10 text-muted-foreground/70 mb-2" />
                <p className="text-sm font-medium text-foreground">
                  {file ? file.name : "Click to select or drag and drop a CSV file"}
                </p>
                <p className="text-xs text-muted-foreground mt-1">
                  {file
                    ? `${(file.size / 1024).toFixed(1)} KB — Ready to import`
                    : "Supports UTF-8 encoded .csv files up to 10MB"}
                </p>
              </div>

              {file && (
                <div className="flex items-center justify-between text-xs rounded-md bg-muted/60 px-3 py-2">
                  <span className="truncate font-mono">{file.name}</span>
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    onClick={handleReset}
                    className="h-6 px-1.5 text-muted-foreground hover:text-foreground"
                  >
                    <X className="size-3.5 mr-1" /> Remove
                  </Button>
                </div>
              )}
            </div>
          )}

          {/* Step 3: Result Summary & Error Table */}
          {result && (
            <div className="space-y-4">
              <div
                className={`rounded-xl border p-4 flex items-start gap-3 ${
                  result.failedCount === 0
                    ? "border-emerald-200 bg-emerald-50/50 text-emerald-900 dark:border-emerald-800 dark:bg-emerald-950/20 dark:text-emerald-200"
                    : result.importedCount > 0
                    ? "border-amber-200 bg-amber-50/50 text-amber-900 dark:border-amber-800 dark:bg-amber-950/20 dark:text-amber-200"
                    : "border-destructive/30 bg-destructive/10 text-destructive"
                }`}
              >
                {result.failedCount === 0 ? (
                  <CheckCircle2 className="size-5 shrink-0 text-emerald-600 mt-0.5" />
                ) : result.importedCount > 0 ? (
                  <AlertTriangle className="size-5 shrink-0 text-amber-600 mt-0.5" />
                ) : (
                  <AlertCircle className="size-5 shrink-0 text-destructive mt-0.5" />
                )}
                <div>
                  <h4 className="text-sm font-semibold">
                    {result.failedCount === 0
                      ? "Import Completed Successfully"
                      : result.importedCount > 0
                      ? "Import Completed with Warnings"
                      : "Import Failed"}
                  </h4>
                  <p className="text-xs mt-1">
                    Processed {result.totalRows} rows:{" "}
                    <strong>{result.importedCount}</strong> imported successfully,{" "}
                    <strong>{result.failedCount}</strong> failed.
                  </p>
                </div>
              </div>

              {result.errors.length > 0 && (
                <div className="space-y-2">
                  <h5 className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                    Row-Level Errors ({result.errors.length})
                  </h5>
                  <div className="max-h-56 overflow-y-auto rounded-lg border">
                    <Table>
                      <TableHeader className="bg-muted/50 sticky top-0">
                        <TableRow>
                          <TableHead className="w-16 text-xs">Row</TableHead>
                          <TableHead className="w-28 text-xs">SKU</TableHead>
                          <TableHead className="w-36 text-xs">Item Name</TableHead>
                          <TableHead className="text-xs">Reason</TableHead>
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {result.errors.map((err, idx) => (
                          <TableRow key={idx} className="text-xs">
                            <TableCell className="font-mono text-muted-foreground">
                              #{err.rowNumber}
                            </TableCell>
                            <TableCell className="font-mono truncate max-w-[100px]">
                              {err.sku || "—"}
                            </TableCell>
                            <TableCell className="truncate max-w-[140px]">
                              {err.itemName || "—"}
                            </TableCell>
                            <TableCell className="text-destructive font-medium">
                              {err.error}
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>

        <DialogFooter className="gap-2 sm:gap-0">
          {result ? (
            <div className="flex w-full items-center justify-between">
              <Button type="button" variant="outline" size="sm" onClick={handleReset}>
                Import Another File
              </Button>
              <Button type="button" size="sm" onClick={() => handleOpenChange(false)}>
                Done
              </Button>
            </div>
          ) : (
            <div className="flex w-full items-center justify-end gap-2">
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => handleOpenChange(false)}
                disabled={isUploading}
              >
                Cancel
              </Button>
              <Button
                type="button"
                size="sm"
                onClick={handleUpload}
                disabled={!file || isUploading}
                className="gap-1.5"
              >
                {isUploading ? (
                  <Loader2 className="size-3.5 animate-spin" />
                ) : (
                  <Upload className="size-3.5" />
                )}
                Import Items
              </Button>
            </div>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

