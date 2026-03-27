// AuditService.cs — Persists audit entries to the database.
// Accountability: all security-relevant actions are durably recorded.
// Confidentiality: details field must never carry passwords, tokens, or raw PII.
using LooseNotes.Data;
using LooseNotes.Models;
using Microsoft.AspNetCore.Http;

namespace LooseNotes.Services;

/// <summary>Database-backed audit logger. Captures actor, action, resource, and IP.</summary>
public sealed class AuditService : IAuditService
{
    private readonly ApplicationDbContext _db;
    private readonly IHttpContextAccessor _httpContextAccessor;
    private readonly ILogger<AuditService> _logger;

    public AuditService(
        ApplicationDbContext db,
        IHttpContextAccessor httpContextAccessor,
        ILogger<AuditService> logger)
    {
        _db = db;
        _httpContextAccessor = httpContextAccessor;
        _logger = logger;
    }

    /// <inheritdoc/>
    public async Task LogAsync(
        string? actorUserId,
        string action,
        string? resourceType = null,
        string? resourceId = null,
        string? details = null)
    {
        var ipAddress = ResolveClientIp();

        var entry = new AuditLog
        {
            ActorUserId = actorUserId,
            Action = action,
            ResourceType = resourceType,
            ResourceId = resourceId,
            Details = details,
            IpAddress = ipAddress
        };

        try
        {
            _db.AuditLogs.Add(entry);
            await _db.SaveChangesAsync();

            // Structured log mirrors DB record — Transparency principle
            _logger.LogInformation(
                "AUDIT | Action={Action} Actor={ActorId} Resource={ResourceType}/{ResourceId} IP={Ip}",
                action, actorUserId ?? "anonymous", resourceType, resourceId, ipAddress);
        }
        catch (Exception ex)
        {
            // Resilience: audit failure must not break the primary operation
            _logger.LogError(ex, "Failed to persist audit log for action {Action}", action);
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private string? ResolveClientIp()
    {
        var context = _httpContextAccessor.HttpContext;
        if (context is null) return null;

        // Trust X-Forwarded-For only if behind a known proxy in production
        var forwarded = context.Request.Headers["X-Forwarded-For"].FirstOrDefault();
        if (!string.IsNullOrEmpty(forwarded))
        {
            // Take only the first IP (client) — not proxies further down the chain
            return forwarded.Split(',')[0].Trim();
        }

        return context.Connection.RemoteIpAddress?.ToString();
    }
}
