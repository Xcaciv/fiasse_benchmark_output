using LooseNotes.Models;
using Microsoft.AspNetCore.Identity.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore;

namespace LooseNotes.Data;

/// <summary>
/// EF Core database context. All queries go through parameterized LINQ/EF (Integrity).
/// Indexes placed on high-frequency filter fields to support Availability.
/// </summary>
public class ApplicationDbContext : IdentityDbContext<ApplicationUser>
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
            entity.HasIndex(n => n.OwnerId);
            // Compound index to optimize "owned notes + public notes" search query
            entity.HasIndex(n => new { n.OwnerId, n.IsPublic });

            entity.HasOne(n => n.Owner)
                  .WithMany(u => u.Notes)
                  .HasForeignKey(n => n.OwnerId)
                  .OnDelete(DeleteBehavior.Cascade);
        });
    }

    private static void ConfigureAttachmentEntity(ModelBuilder builder)
    {
        builder.Entity<Attachment>(entity =>
        {
            entity.HasIndex(a => a.NoteId);
            entity.HasIndex(a => a.StoredFileName).IsUnique();

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
            // Enforce one rating per user per note at DB level (Integrity)
            entity.HasIndex(r => new { r.NoteId, r.RaterId }).IsUnique();

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
            entity.HasIndex(s => s.Token).IsUnique();
            entity.HasIndex(s => s.NoteId);

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
            entity.HasIndex(a => a.UserId);
            entity.HasIndex(a => a.OccurredAt);
            entity.HasIndex(a => a.Action);
        });
    }
}
