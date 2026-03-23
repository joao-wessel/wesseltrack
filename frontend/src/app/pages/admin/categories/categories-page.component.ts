import { Component, OnDestroy, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ConfirmDialogService } from '../../../core/confirm-dialog.service';
import { FinanceService } from '../../../core/finance.service';
import { Category } from '../../../core/models';
import { ToastService } from '../../../core/toast.service';

@Component({
  selector: 'app-categories-page',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './categories-page.component.html',
  styleUrl: './categories-page.component.scss'
})
export class CategoriesPageComponent implements OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly confirmDialog = inject(ConfirmDialogService);
  private readonly financeService = inject(FinanceService);
  private readonly toastService = inject(ToastService);

  readonly categories = signal<Category[]>([]);
  readonly editingId = signal<number | null>(null);
  readonly compactLayout = signal(this.isCompactViewport());
  readonly mobileFormOpen = signal(false);
  readonly showForm = computed(() => !this.compactLayout() || this.mobileFormOpen());
  readonly form = this.fb.nonNullable.group({
    name: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(60)]),
    color: this.fb.nonNullable.control('#0f766e', Validators.required)
  });

  constructor() {
    if (typeof window !== 'undefined') {
      window.addEventListener('resize', this.handleViewportResize, { passive: true });
    }
    this.load();
  }

  ngOnDestroy() {
    if (typeof window !== 'undefined') {
      window.removeEventListener('resize', this.handleViewportResize);
    }
  }

  save() {
    if (this.form.invalid) {
      this.mobileFormOpen.set(true);
      this.form.markAllAsTouched();
      this.toastService.error('Preencha corretamente os campos da categoria.');
      return;
    }

    const request = this.editingId()
      ? this.financeService.updateCategory(this.editingId()!, this.form.getRawValue())
      : this.financeService.createCategory(this.form.getRawValue());

    request.subscribe({
      next: () => {
        this.toastService.success('Categoria salva com sucesso.');
        this.cancel();
        this.load();
      },
      error: (error: any) => this.toastService.error(error?.error?.error ?? 'Falha ao salvar categoria.')
    });
  }

  edit(category: Category) {
    this.editingId.set(category.id);
    this.mobileFormOpen.set(true);
    this.form.patchValue(category);
  }

  async remove(category: Category) {
    const confirmed = await this.confirmDialog.open({
      title: 'Excluir categoria',
      message: `Confirma a exclus\u00E3o da categoria "${category.name}"? Esta a\u00E7\u00E3o n\u00E3o poder\u00E1 ser desfeita.`,
      confirmLabel: 'Excluir',
      cancelLabel: 'Cancelar',
      variant: 'danger'
    });

    if (!confirmed) {
      return;
    }

    this.financeService.deleteCategory(category.id).subscribe({
      next: () => {
        this.toastService.success('Categoria exclu\u00EDda com sucesso.');
        this.load();
      },
      error: (error: any) => this.toastService.error(error?.error?.error ?? 'Falha ao excluir categoria.')
    });
  }

  cancel() {
    this.editingId.set(null);
    this.form.reset({ name: '', color: '#0f766e' });
    if (this.compactLayout()) {
      this.mobileFormOpen.set(false);
    }
  }

  toggleMobileForm() {
    this.mobileFormOpen.update((value) => !value);
  }

  private load() {
    this.financeService.getCategories().subscribe({
      next: (categories) => this.categories.set(categories),
      error: () => this.toastService.error('N\u00E3o foi poss\u00EDvel carregar as categorias.')
    });
  }

  private readonly handleViewportResize = () => {
    this.compactLayout.set(this.isCompactViewport());
  };

  private isCompactViewport(): boolean {
    return typeof window !== 'undefined' && window.innerWidth <= 720;
  }
}
