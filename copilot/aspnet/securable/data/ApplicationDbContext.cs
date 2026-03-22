using Microsoft.AspNetCore.Identity.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore;
using LooseNotes.Models;

namespace LooseNotes.Data;

public sealed class ApplicationDbContext : IdentityDbContext<ApplicationUser>
{
    public ApplicationDbContext(DbContextOptions<ApplicationDbContext> options)
        : base(options)
    {
    }

    public DbSet<Note> Notes => Set<Note>();
    public DbSet<Attachment> Attachments => Set<Attachment>();
    public DbSet<Rating> Ratings => Set<Rating>();
    public DbSet<ShareLink> ShareLinks => Set<ShareLink>();
    public DbSet<ActivityLog> ActivityLogs => Set<ActivityLog>();

    protected override void OnModelCreating(ModelBuilder builder)
    {
        base.OnModelCreating(builder);

        builder.Entity<ApplicationUser>(entity =>
        {
            entity.Property(x => x.RegisteredAtUtc).HasDefaultValueSql("CURRENT_TIMESTAMP");
        });

        builder.Entity<Note>(entity =>
        {
            entity.HasIndex(x => x.OwnerId);
            entity.HasIndex(x => x.Title);
            entity.HasIndex(x => new { x.IsPublic, x.CreatedAtUtc });
            entity.Property(x => x.Title).HasMaxLength(120);
            entity.Property(x => x.Content).HasMaxLength(20000);
            entity.HasOne(x => x.Owner)
                .WithMany(x => x.OwnedNotes)
                .HasForeignKey(x => x.OwnerId)
                .OnDelete(DeleteBehavior.Cascade);
        });

        builder.Entity<Attachment>(entity =>
        {
            entity.HasIndex(x => x.NoteId);
            entity.Property(x => x.StoredFileName).HasMaxLength(260);
            entity.Property(x => x.OriginalFileName).HasMaxLength(260);
            entity.Property(x => x.ContentType).HasMaxLength(256);
            entity.HasOne(x => x.Note)
                .WithMany(x => x.Attachments)
                .HasForeignKey(x => x.NoteId)
                .OnDelete(DeleteBehavior.Cascade);
        });

        builder.Entity<Rating>(entity =>
        {
            entity.HasIndex(x => new { x.NoteId, x.UserId }).IsUnique();
            entity.HasIndex(x => x.CreatedAtUtc);
            entity.Property(x => x.Comment).HasMaxLength(1000);
            entity.HasOne(x => x.Note)
                .WithMany(x => x.Ratings)
                .HasForeignKey(x => x.NoteId)
                .OnDelete(DeleteBehavior.Cascade);
            entity.HasOne(x => x.User)
                .WithMany(x => x.Ratings)
                .HasForeignKey(x => x.UserId)
                .OnDelete(DeleteBehavior.Restrict);
        });

        builder.Entity<ShareLink>(entity =>
        {
            entity.HasIndex(x => x.NoteId);
            entity.HasIndex(x => x.TokenHash).IsUnique();
            entity.Property(x => x.TokenHash).HasMaxLength(128);
            entity.Property(x => x.ProtectedToken).HasMaxLength(1024);
            entity.HasOne(x => x.Note)
                .WithMany(x => x.ShareLinks)
                .HasForeignKey(x => x.NoteId)
                .OnDelete(DeleteBehavior.Cascade);
        });

        builder.Entity<ActivityLog>(entity =>
        {
            entity.HasIndex(x => x.CreatedAtUtc);
            entity.Property(x => x.ActionType).HasMaxLength(64);
            entity.Property(x => x.Description).HasMaxLength(512);
            entity.Property(x => x.IpAddress).HasMaxLength(64);
        });
    }
}
