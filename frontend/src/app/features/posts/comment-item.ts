import { ChangeDetectionStrategy, Component, inject, input, linkedSignal, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Comment } from '../../core/models/comment.model';
import { AuthService } from '../../core/services/auth.service';
import { CommentService } from '../../core/services/comment.service';
import { errorMessage } from '../../shared/api-error';
import { Avatar } from '../../shared/avatar';
import { RelativeTimePipe } from '../../shared/relative-time.pipe';
import { ReportButton } from '../../shared/report-button';

/**
 * Un comentario con sus respuestas.
 *
 * Las respuestas se piden solo cuando alguien las abre. En un hilo largo, traerlas
 * todas por adelantado significa descargar la conversación entera para enseñar los
 * primeros veinte comentarios.
 *
 * El anidamiento se queda en un nivel a propósito: las respuestas a una respuesta
 * cuelgan del mismo hilo en lugar de abrir otro, que es lo que hace que los hilos
 * profundos se vuelvan ilegibles en el móvil.
 */
@Component({
  selector: 'app-comment-item',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink, Avatar, RelativeTimePipe, ReportButton],
  template: `
    <article class="comment" [class.comment--reply]="isReply()">
      <a class="comment__author" [routerLink]="['/u', comment().authorUsername]">
        <app-avatar [username]="comment().authorUsername" [size]="28" />
        <span>{{ comment().authorUsername }}</span>
      </a>
      <time [attr.datetime]="comment().createdAt">
        {{ comment().createdAt | relativeTime }}@if (edited()) {<span class="badge">editado</span>}
      </time>

      @if (editing()) {
        <form class="comment__form" [formGroup]="editForm" (ngSubmit)="saveEdit()" novalidate>
          <textarea formControlName="content" rows="3" maxlength="1000"></textarea>
          <div class="comment__form-actions">
            <button type="submit" class="button button--small" [disabled]="editForm.invalid">Guardar</button>
            <button type="button" class="button button--ghost button--small" (click)="editing.set(false)">
              Cancelar
            </button>
          </div>
        </form>
      } @else {
        <p class="comment__text">{{ content() }}</p>

        <div class="comment__actions">
          @if (!isReply() && isLoggedIn()) {
            <button type="button" class="link-button" (click)="toggleReplyForm()">Responder</button>
          }
          @if (comment().own) {
            <button type="button" class="link-button" (click)="startEdit()">Editar</button>
            <button type="button" class="link-button link-button--danger" (click)="remove()">Borrar</button>
          } @else if (isLoggedIn()) {
            <app-report-button targetType="COMMENT" [targetId]="comment().id" compact />
          }
        </div>
      }

      @if (error()) {
        <p class="error comment__error" role="alert">{{ error() }}</p>
      }

      @if (replying()) {
        <form class="comment__form" [formGroup]="replyForm" (ngSubmit)="sendReply()" novalidate>
          <textarea
            formControlName="content"
            rows="2"
            maxlength="1000"
            [attr.placeholder]="'Responder a ' + comment().authorUsername + '…'"
          ></textarea>
          <div class="comment__form-actions">
            <button type="submit" class="button button--small" [disabled]="replyForm.invalid || sending()">
              {{ sending() ? 'Enviando…' : 'Responder' }}
            </button>
            <button type="button" class="button button--ghost button--small" (click)="replying.set(false)">
              Cancelar
            </button>
          </div>
        </form>
      }

      @if (!isReply() && visibleReplyCount() > 0) {
        <div class="comment__replies">
          @if (!repliesOpen()) {
            <button type="button" class="link-button" (click)="openReplies()">
              Ver {{ visibleReplyCount() }}
              {{ visibleReplyCount() === 1 ? 'respuesta' : 'respuestas' }}
            </button>
          } @else {
            @for (reply of replies(); track reply.id) {
              <app-comment-item [comment]="reply" isReply (removed)="dropReply(reply.id)" />
            }
            <button type="button" class="link-button" (click)="repliesOpen.set(false)">Ocultar respuestas</button>
          }
        </div>
      }
    </article>
  `,
  styles: `
    .comment {
      display: grid;
      grid-template-columns: auto 1fr;
      gap: 0.25rem 0.75rem;
      padding: 0.85rem 0;
      border-top: 1px solid var(--border);
    }
    .comment--reply {
      border-top: none;
      padding: 0.6rem 0 0.2rem;
      border-left: 2px solid var(--border);
      padding-left: 0.9rem;
      margin-left: 0.2rem;
    }
    .comment__author {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      font-weight: 600;
      font-size: 0.9rem;
      color: inherit;
      text-decoration: none;
    }
    .comment__author:hover span { color: var(--accent); }
    .comment time { color: var(--text-dim); font-size: 0.8rem; align-self: center; }
    .badge {
      background: var(--surface-3);
      border-radius: 999px;
      padding: 0.1rem 0.5rem;
      font-size: 0.72rem;
      margin-left: 0.4rem;
    }

    .comment__text { grid-column: 1 / -1; margin: 0.25rem 0 0; line-height: 1.6; white-space: pre-wrap; }
    .comment__actions { grid-column: 1 / -1; display: flex; align-items: center; gap: 0.75rem; margin-top: 0.35rem; }
    .comment__error { grid-column: 1 / -1; margin-top: 0.35rem; }
    .comment__form { grid-column: 1 / -1; display: grid; gap: 0.5rem; margin-top: 0.5rem; }
    .comment__form-actions { display: flex; gap: 0.5rem; }
    .comment__replies { grid-column: 1 / -1; margin-top: 0.5rem; display: grid; gap: 0.2rem; justify-items: start; }

    .link-button {
      background: none;
      border: none;
      padding: 0;
      font: inherit;
      font-size: 0.82rem;
      color: var(--text-dim);
      cursor: pointer;
      text-decoration: underline;
    }
    .link-button:hover { color: var(--text); }
    .link-button--danger:hover { color: var(--danger); }
  `,
})
export class CommentItem {
  private readonly commentService = inject(CommentService);
  private readonly auth = inject(AuthService);
  private readonly formBuilder = inject(FormBuilder);

  readonly comment = input.required<Comment>();
  readonly isReply = input(false, { transform: (value: unknown) => value !== false });

  readonly removed = output<number>();

  protected readonly isLoggedIn = this.auth.isLoggedIn;

  /**
   * Copia local del texto y de la marca de edición.
   *
   * linkedSignal sigue al input mientras no se toque, y admite escritura local
   * cuando el autor edita. Mutar el objeto del input no valdría: con OnPush la
   * referencia no cambia y la vista no se enteraría.
   */
  protected readonly content = linkedSignal(() => this.comment().content);
  protected readonly edited = linkedSignal(() => this.comment().edited);

  protected readonly editing = signal(false);
  protected readonly replying = signal(false);
  protected readonly repliesOpen = signal(false);
  protected readonly sending = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly replies = signal<Comment[]>([]);

  /** Las respuestas recién enviadas se suman al contador que llegó del servidor. */
  private readonly addedReplies = signal(0);

  protected visibleReplyCount(): number {
    return this.repliesOpen() ? this.replies().length : this.comment().replyCount + this.addedReplies();
  }

  protected readonly editForm = this.formBuilder.nonNullable.group({
    content: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(1000)]],
  });

  protected readonly replyForm = this.formBuilder.nonNullable.group({
    content: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(1000)]],
  });

  protected startEdit(): void {
    this.editForm.setValue({ content: this.content() });
    this.editing.set(true);
  }

  protected saveEdit(): void {
    if (this.editForm.invalid) {
      return;
    }

    this.commentService.update(this.comment().id, this.editForm.getRawValue().content).subscribe({
      next: (updated) => {
        this.content.set(updated.content);
        this.edited.set(true);
        this.editing.set(false);
      },
      error: (err: unknown) => this.error.set(errorMessage(err)),
    });
  }

  protected remove(): void {
    if (!confirm('¿Seguro que quieres borrar este comentario?')) {
      return;
    }

    this.commentService.remove(this.comment().id).subscribe({
      next: () => this.removed.emit(this.comment().id),
      error: (err: unknown) => this.error.set(errorMessage(err)),
    });
  }

  protected toggleReplyForm(): void {
    this.replying.update((open) => !open);
  }

  protected openReplies(): void {
    this.repliesOpen.set(true);

    if (this.replies().length) {
      return;
    }

    this.commentService.replies(this.comment().id).subscribe({
      next: (page) => this.replies.set(page.content),
      error: (err: unknown) => this.error.set(errorMessage(err)),
    });
  }

  protected sendReply(): void {
    if (this.replyForm.invalid || this.sending()) {
      return;
    }

    this.sending.set(true);
    this.error.set(null);

    this.commentService
      .create({
        content: this.replyForm.getRawValue().content,
        postId: this.comment().postId,
        parentId: this.comment().id,
      })
      .subscribe({
        next: (reply) => {
          this.sending.set(false);
          this.replying.set(false);
          this.replyForm.reset();
          this.addedReplies.update((count) => count + 1);
          if (this.repliesOpen()) {
            this.replies.update((list) => [...list, reply]);
          }
        },
        error: (err: unknown) => {
          this.sending.set(false);
          this.error.set(errorMessage(err));
        },
      });
  }

  protected dropReply(id: number): void {
    this.replies.update((list) => list.filter((reply) => reply.id !== id));
    this.addedReplies.update((count) => count - 1);
  }
}
