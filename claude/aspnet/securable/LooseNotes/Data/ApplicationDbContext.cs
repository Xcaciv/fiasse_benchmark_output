using LooseNotes.Models;
using Microsoft.AspNetCore.Identity.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore;

namespace LooseNotes.Data;

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

        // Notes
        builder.Entity<Note>(e =>
        {
            e.HasIndex(n => n.OwnerId);
            e.HasIndex(n => n.IsPublic);
            e.HasIndex(n => n.CreatedAt);
            e.HasOne(n => n.Owner)
             .WithMany(u => u.Notes)
             .HasForeignKey(n => n.OwnerId)
             .OnDelete(DeleteBehavior.Cascade);
        });

        // Attachments
        builder.Entity<Attachment>(e =>
        {
            e.HasOne(a => a.Note)
             .WithMany(n => n.Attachments)
             .HasForeignKey(a => a.NoteId)
             .OnDelete(DeleteBehavior.Cascade);
        });

        // Ratings – one rating per user per note
        builder.Entity<Rating>(e =>
        {
            e.HasIndex(r => new { r.NoteId, r.RaterId }).IsUnique();
            e.HasOne(r => r.Note)
             .WithMany(n => n.Ratings)
             .HasForeignKey(r => r.NoteId)
             .OnDelete(DeleteBehavior.Cascade);
            e.HasOne(r => r.Rater)
             .WithMany(u => u.Ratings)
             .HasForeignKey(r => r.RaterId)
             .OnDelete(DeleteBehavior.Restrict);
        });

        // ShareLinks
        builder.Entity<ShareLink>(e =>
        {
            e.HasIndex(s => s.Token).IsUnique();
            e.HasOne(s => s.Note)
             .WithMany(n => n.ShareLinks)
             .HasForeignKey(s => s.NoteId)
             .OnDelete(DeleteBehavior.Cascade);
        });

        // AuditLog
        builder.Entity<AuditLog>(e =>
        {
            e.HasIndex(a => a.OccurredAt);
            e.HasIndex(a => a.ActorId);
            e.HasIndex(a => a.EventType);
        });
    }
}
