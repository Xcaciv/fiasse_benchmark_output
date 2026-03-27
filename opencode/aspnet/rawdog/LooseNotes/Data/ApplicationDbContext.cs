using Microsoft.AspNetCore.Identity.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore;
using LooseNotes.Models;

namespace LooseNotes.Data;

public class ApplicationDbContext : IdentityDbContext<ApplicationUser>
{
    public ApplicationDbContext(DbContextOptions<ApplicationDbContext> options)
        : base(options)
    {
    }

    public DbSet<Note> Notes { get; set; }
    public DbSet<Attachment> Attachments { get; set; }
    public DbSet<Rating> Ratings { get; set; }
    public DbSet<ShareLink> ShareLinks { get; set; }
    public DbSet<ActivityLog> ActivityLogs { get; set; }

    protected override void OnModelCreating(ModelBuilder builder)
    {
        base.OnModelCreating(builder);

        builder.Entity<ApplicationUser>(entity =>
        {
            entity.HasIndex(e => e.UserName).IsUnique();
            entity.HasIndex(e => e.Email).IsUnique();
        });

        builder.Entity<Note>(entity =>
        {
            entity.HasIndex(e => e.Title);
            entity.HasIndex(e => e.CreatedAt);
            entity.HasIndex(e => e.UserId);
            entity.HasIndex(e => e.IsPublic);
            
            entity.HasOne(n => n.User)
                .WithMany(u => u.Notes)
                .HasForeignKey(n => n.UserId)
                .OnDelete(DeleteBehavior.Cascade);
        });

        builder.Entity<Attachment>(entity =>
        {
            entity.HasIndex(e => e.NoteId);
            
            entity.HasOne(a => a.Note)
                .WithMany(n => n.Attachments)
                .HasForeignKey(a => a.NoteId)
                .OnDelete(DeleteBehavior.Cascade);
        });

        builder.Entity<Rating>(entity =>
        {
            entity.HasIndex(e => e.NoteId);
            entity.HasIndex(e => e.UserId);
            entity.HasIndex(e => new { e.UserId, e.NoteId }).IsUnique();
            
            entity.HasOne(r => r.User)
                .WithMany(u => u.Ratings)
                .HasForeignKey(r => r.UserId)
                .OnDelete(DeleteBehavior.Cascade);
            
            entity.HasOne(r => r.Note)
                .WithMany(n => n.Ratings)
                .HasForeignKey(r => r.NoteId)
                .OnDelete(DeleteBehavior.Cascade);
        });

        builder.Entity<ShareLink>(entity =>
        {
            entity.HasIndex(e => e.Token).IsUnique();
            entity.HasIndex(e => e.NoteId);
            
            entity.HasOne(s => s.Note)
                .WithMany(n => n.ShareLinks)
                .HasForeignKey(s => s.NoteId)
                .OnDelete(DeleteBehavior.Cascade);
        });

        builder.Entity<ActivityLog>(entity =>
        {
            entity.HasIndex(e => e.CreatedAt);
            entity.HasIndex(e => e.UserId);
        });
    }
}
