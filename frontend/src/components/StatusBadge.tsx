import type { CSSProperties } from "react";

const STATUS_CONFIG: Record<string, { bg: string; text: string; label: string }> = {
  CREATED: { bg: "#F1F5F9", text: "#64748B", label: "Pending" },
  PAYMENT_PENDING: { bg: "#FEF3C7", text: "#D97706", label: "Awaiting Payment" },
  CONFIRMED: { bg: "#ECFDF5", text: "#10B981", label: "Confirmed" },
  ASSIGNED: { bg: "#E0F2FE", text: "#0284C7", label: "Rider Assigned" },
  OUT_FOR_DELIVERY: { bg: "#EEF2FF", text: "#4F46E5", label: "Out for Delivery" },
  DELIVERED: { bg: "#DCFCE7", text: "#16A34A", label: "Delivered" },
  FAILED: { bg: "#FEF2F2", text: "#EF4444", label: "Failed" },
  CANCELLED: { bg: "#F1F5F9", text: "#475569", label: "Cancelled" },
};

export default function StatusBadge({ status }: { status: string }) {
  const config = STATUS_CONFIG[status] || {
    bg: "#F1F5F9",
    text: "#64748B",
    label: status,
  };

  return (
    <span
      style={{
        backgroundColor: config.bg,
        color: config.text,
        padding: "4px 10px",
        borderRadius: "var(--radius-xl)",
        fontSize: "0.75rem",
        fontWeight: 600,
        letterSpacing: "0.3px",
      }}
    >
      {config.label}
    </span>
  );
}
