// ApplicationDbContext.cs — EF Core context. All queries use parameterized LINQ (Integrity).
// Analyzability: each entity configuration is in its own method.
using LooseNotes.Models;
using Microsoft.AspNetCore.Identity.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore;

namespace LooseNotes.Data;

/// <summary>Primary EF Core context. All DB access goes through this context to ensure
/// parameterized queries (prevents SQL injection — Integrity pillar).</summary>
public sealed class ApplicationDbContext : IdentityDbContext<ApplicationUser>
{
    public ApplicationDbContext(DbContextOptions<ApplicationDbContext> options)
        : base(options) { }

    public DbSet<Note> Notes => Set<Note>();
    public DbSet<Attachment> Attachments => Set<Attachment>();
    public DbSet<Rating> Ratings => Set<Rating>();
    public DbSet<ShareLink> ShareLinks => Set<ShareLink>();
    public DbSet<AuditLog> AuditLogs => Set<AuditLog>();

    protected override void OnModelCreating(ModelBuilder builder)
    {
        base.OnModelCreating(builder);

        ConfigureNote(builder);
        ConfigureAttachment(builder);
        ConfigureRating(builder);
        ConfigureShareLink(builder);
        ConfigureAuditLog(builder);
    }

    // ── Entity configurations ─────────────────────────────────────────────────

    private static void ConfigureNote(ModelBuilder builder)
    {
        builder.Entity<Note>(e =>
        {
            e.HasKey(n => n.Id);
            e.Property(n => n.Title).HasMaxLength(300).IsRequired();
            e.Property(n => n.Content).IsRequired();
            e.Property(n => n.UserId).IsRequired();

            // Index for search performance (Availability)
            e.HasIndex(n => n.UserId);
            e.HasIndex(n => n.Visibility);

            e.HasOne(n => n.User)
             .WithMany(u => u.Notes)
             .HasForeignKey(n => n.UserId)
             .OnDelete(DeleteBehavior.Cascade);
        });
    }

    private static void ConfigureAttachment(ModelBuilder builder)
    {
        builder.Entity<Attachment>(e =>
        {
            e.HasKey(a => a.Id);
            e.Property(a => a.OriginalFileName).HasMaxLength(500).IsRequired();
            // StoredFileName is a UUID — no path traversal possible
            e.Property(a => a.StoredFileName).HasMaxLength(50).IsRequired();
            e.Property(a => a.ContentType).HasMaxLength(200).IsRequired();

            e.HasOne(a => a.Note)
             .WithMany(n => n.Attachments)
             .HasForeignKey(a => a.NoteId)
             .OnDelete(DeleteBehavior.Cascade);
        });
    }

    private static void ConfigureRating(ModelBuilder builder)
    {
        builder.Entity<Rating>(e =>
        {
            e.HasKey(r => r.Id);
            e.Property(r => r.Value).IsRequired();
            // Check constraint: value must be 1–5 (Integrity at DB level)
            e.ToTable(t => t.HasCheckConstraint("CK_Rating_Value", "[Value] >= 1 AND [Value] <= 5"));
            e.Property(r => r.Comment).HasMaxLength(1000);

            // One rating per user per note (Integrity — no duplicate ratings)
            e.HasIndex(r => new { r.NoteId, r.UserId }).IsUnique();

            e.HasOne(r => r.Note)
             .WithMany(n => n.Ratings)
             .HasForeignKey(r => r.NoteId)
             .OnDelete(DeleteBehavior.Cascade);

            e.HasOne(r => r.User)
             .WithMany(u => u.Ratings)
             .HasForeignKey(r => r.UserId)
             .OnDelete(DeleteBehavior.Cascade);
        });
    }

    private static void ConfigureShareLink(ModelBuilder builder)
    {
        builder.Entity<ShareLink>(e =>
        {
            e.HasKey(s => s.Id);
            // Token indexed for O(1) lookup — Availability
            e.Property(s => s.Token).HasMaxLength(64).IsRequired();
            e.HasIndex(s => s.Token).IsUnique();

            e.HasOne(s => s.Note)
             .WithMany(n => n.ShareLinks)
             .HasForeignKey(s => s.NoteId)
             .OnDelete(DeleteBehavior.Cascade);
        });
    }

    private static void ConfigureAuditLog(ModelBuilder builder)
    {
        builder.Entity<AuditLog>(e =>
        {
            e.HasKey(a => a.Id);
            e.Property(a => a.Action).HasMaxLength(200).IsRequired();
            e.Property(a => a.ResourceType).HasMaxLength(100);
            e.Property(a => a.ResourceId).HasMaxLength(100);
            e.Property(a => a.IpAddress).HasMaxLength(45); // IPv6 max length
            e.Property(a => a.Details).HasMaxLength(2000);

            e.HasIndex(a => a.ActorUserId);
            e.HasIndex(a => a.OccurredAt);

            // Nullable FK — audit logs survive user deletion for compliance
            e.HasOne(a => a.Actor)
             .WithMany(u => u.AuditLogs)
             .HasForeignKey(a => a.ActorUserId)
             .OnDelete(DeleteBehavior.SetNull)
             .IsRequired(false);
        });
    }
}
