using LooseNotes.Models;
using Microsoft.AspNetCore.Identity.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore;

namespace LooseNotes.Data;

public class ApplicationDbContext : IdentityDbContext<ApplicationUser>
{
    public ApplicationDbContext(DbContextOptions<ApplicationDbContext> options)
        : base(options)
    {
    }

    public DbSet<Note> Notes => Set<Note>();
    public DbSet<Attachment> Attachments => Set<Attachment>();
    public DbSet<Rating> Ratings => Set<Rating>();
    public DbSet<ShareLink> ShareLinks => Set<ShareLink>();

    protected override void OnModelCreating(ModelBuilder builder)
    {
        base.OnModelCreating(builder);

        builder.Entity<Note>(entity =>
        {
            entity.HasIndex(n => n.OwnerId);
            entity.HasIndex(n => n.IsPublic);
            entity.Property(n => n.Title).HasMaxLength(200).IsRequired();
            entity.Property(n => n.Content).HasMaxLength(50000).IsRequired();
            entity.Property(n => n.OwnerId).IsRequired();
        });

        builder.Entity<Attachment>(entity =>
        {
            entity.Property(a => a.OriginalFileName).HasMaxLength(260).IsRequired();
            entity.Property(a => a.StoredFileName).HasMaxLength(260).IsRequired();
            entity.Property(a => a.ContentType).HasMaxLength(100).IsRequired();
        });

        builder.Entity<Rating>(entity =>
        {
            entity.HasIndex(r => r.NoteId);
            entity.HasIndex(r => new { r.NoteId, r.RaterId }).IsUnique();
            entity.Property(r => r.Value).IsRequired();
            entity.Property(r => r.Comment).HasMaxLength(1000);
        });

        builder.Entity<ShareLink>(entity =>
        {
            entity.HasIndex(s => s.Token).IsUnique();
            entity.Property(s => s.Token).HasMaxLength(100).IsRequired();
        });

        builder.Entity<ApplicationUser>(entity =>
        {
            entity.Property(u => u.DisplayName).HasMaxLength(100);
        });
    }
}
