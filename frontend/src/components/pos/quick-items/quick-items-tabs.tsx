"use client";

import * as React from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Plus, X } from "lucide-react";
import { cn } from "@/lib/utils";

export interface QuickTab {
  id: string;
  name: string;
}

interface QuickItemsTabsProps {
  tabs: QuickTab[];
  activeTabId: string;
  onSelect: (id: string) => void;
  onAdd: (name: string) => void;
  onRemove: (id: string) => void;
  onRename: (id: string, name: string) => void;
}

export function QuickItemsTabs({
  tabs,
  activeTabId,
  onSelect,
  onAdd,
  onRemove,
  onRename,
}: QuickItemsTabsProps) {
  const [adding, setAdding] = React.useState(false);
  const [newName, setNewName] = React.useState("");
  const [editingId, setEditingId] = React.useState<string | null>(null);
  const [editName, setEditName] = React.useState("");

  const commitAdd = () => {
    const name = newName.trim();
    if (name) onAdd(name);
    setNewName("");
    setAdding(false);
  };

  const commitRename = (id: string) => {
    const name = editName.trim();
    if (name) onRename(id, name);
    setEditingId(null);
  };

  return (
    <div className="flex flex-wrap items-center gap-1 rounded-lg border bg-card p-1">
      {tabs.map((tab) => {
        const active = tab.id === activeTabId;
        return (
          <div key={tab.id} className="flex items-center">
            {editingId === tab.id ? (
              <Input
                autoFocus
                value={editName}
                onChange={(e) => setEditName(e.target.value)}
                onBlur={() => commitRename(tab.id)}
                onKeyDown={(e) => {
                  if (e.key === "Enter") commitRename(tab.id);
                  if (e.key === "Escape") setEditingId(null);
                }}
                className="h-8 w-32"
              />
            ) : (
              <button
                type="button"
                onClick={() => onSelect(tab.id)}
                onDoubleClick={() => {
                  setEditingId(tab.id);
                  setEditName(tab.name);
                }}
                className={cn(
                  "rounded-md px-3 py-1.5 text-sm font-medium transition",
                  active
                    ? "bg-primary text-primary-foreground"
                    : "text-muted-foreground hover:bg-muted hover:text-foreground",
                )}
              >
                {tab.name}
              </button>
            )}
            {tabs.length > 1 && (
              <button
                type="button"
                onClick={() => onRemove(tab.id)}
                className="ml-1 rounded p-1 text-muted-foreground hover:text-destructive"
                aria-label={`Remove ${tab.name}`}
              >
                <X className="h-3 w-3" />
              </button>
            )}
          </div>
        );
      })}

      {adding ? (
        <div className="flex items-center gap-1">
          <Input
            autoFocus
            value={newName}
            onChange={(e) => setNewName(e.target.value)}
            onBlur={commitAdd}
            onKeyDown={(e) => {
              if (e.key === "Enter") commitAdd();
              if (e.key === "Escape") {
                setNewName("");
                setAdding(false);
              }
            }}
            placeholder="Tab name"
            className="h-8 w-32"
          />
        </div>
      ) : (
        <Button size="sm" variant="ghost" onClick={() => setAdding(true)}>
          <Plus className="h-4 w-4" />
        </Button>
      )}
    </div>
  );
}
