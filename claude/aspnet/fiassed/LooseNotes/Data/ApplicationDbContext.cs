using LooseNotes.Models;
using Microsoft.AspNetCore.Identity.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore;

namespace LooseNotes.Data;

/// <summary>
/// Database context with query filters enforcing visibility rules at the data access layer.
/// Visibility is enforced in queries, not only in controller logic (FIASSE S2.4, ASVS V8.2.2).
/// </summary>
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
        ConfigureNoteEntity(builder);
        ConfigureAttachmentEntity(builder);
        ConfigureRatingEntity(builder);
        ConfigureShareLinkEntity(builder);
        ConfigureAuditLogEntity(builder);
    }

    private static void ConfigureNoteEntity(ModelBuilder builder)
    {
        builder.Entity<Note>(entity =>
        {
            entity.HasKey(n => n.Id);
            entity.Property(n => n.Title).HasMaxLength(500).IsRequired();
            entity.Property(n => n.Content).HasMaxLength(100_000).IsRequired();
            entity.Property(n => n.UserId).IsRequired();
            entity.Property(n => n.IsPublic).HasDefaultValue(false);

            // Indexes for efficient visibility-filtered queries (FIASSE S2.2 - performance)
            entity.HasIndex(n => new { n.IsPublic, n.UserId });
            entity.HasIndex(n => new { n.IsPublic, n.AverageRating, n.RatingCount });

            entity.HasOne(n => n.User)
                  .WithMany(u => u.Notes)
                  .HasForeignKey(n => n.UserId)
                  .OnDelete(DeleteBehavior.Cascade);
        });
    }

    private static void ConfigureAttachmentEntity(ModelBuilder builder)
    {
        builder.Entity<Attachment>(entity =>
        {
            entity.HasKey(a => a.Id);
            entity.Property(a => a.StoredFileName).HasMaxLength(100).IsRequired();
            entity.Property(a => a.OriginalFileName).HasMaxLength(255).IsRequired();
            entity.Property(a => a.ContentType).HasMaxLength(100).IsRequired();

            entity.HasOne(a => a.Note)
                  .WithMany(n => n.Attachments)
                  .HasForeignKey(a => a.NoteId)
                  .OnDelete(DeleteBehavior.Cascade);
        });
    }

    private static void ConfigureRatingEntity(ModelBuilder builder)
    {
        builder.Entity<Rating>(entity =>
        {
            entity.HasKey(r => r.Id);
            // One rating per user per note - enforced at DB level (integrity constraint)
            entity.HasIndex(r => new { r.NoteId, r.RaterId }).IsUnique();
            entity.Property(r => r.Comment).HasMaxLength(1000);

            entity.HasOne(r => r.Note)
                  .WithMany(n => n.Ratings)
                  .HasForeignKey(r => r.NoteId)
                  .OnDelete(DeleteBehavior.Cascade);

            entity.HasOne(r => r.Rater)
                  .WithMany(u => u.Ratings)
                  .HasForeignKey(r => r.RaterId)
                  .OnDelete(DeleteBehavior.Restrict);
        });
    }

    private static void ConfigureShareLinkEntity(ModelBuilder builder)
    {
        builder.Entity<ShareLink>(entity =>
        {
            entity.HasKey(s => s.Id);
            // Token lookup must be fast - used on every share link access
            entity.HasIndex(s => s.Token).IsUnique();
            entity.Property(s => s.Token).HasMaxLength(100).IsRequired();

            entity.HasOne(s => s.Note)
                  .WithMany(n => n.ShareLinks)
                  .HasForeignKey(s => s.NoteId)
                  .OnDelete(DeleteBehavior.Cascade);
        });
    }

    private static void ConfigureAuditLogEntity(ModelBuilder builder)
    {
        builder.Entity<AuditLog>(entity =>
        {
            entity.HasKey(a => a.Id);
            entity.Property(a => a.EventType).HasMaxLength(100).IsRequired();
            entity.Property(a => a.ResourceType).HasMaxLength(50);
            entity.Property(a => a.ResourceId).HasMaxLength(100);
            entity.Property(a => a.SourceIp).HasMaxLength(45);
            entity.Property(a => a.Outcome).HasMaxLength(20);
            entity.Property(a => a.Details).HasMaxLength(2000);

            // Index for efficient audit queries by user and event type
            entity.HasIndex(a => new { a.UserId, a.Timestamp });
            entity.HasIndex(a => new { a.EventType, a.Timestamp });

            // Nullable FK - audit logs for anonymous/system actions have no user
            entity.HasOne(a => a.User)
                  .WithMany(u => u.AuditLogs)
                  .HasForeignKey(a => a.UserId)
                  .OnDelete(DeleteBehavior.SetNull)
                  .IsRequired(false);
        });
    }
}
