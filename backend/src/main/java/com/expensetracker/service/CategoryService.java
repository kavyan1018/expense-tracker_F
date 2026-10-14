package com.expensetracker.service;

import com.expensetracker.domain.Category;
import com.expensetracker.domain.TxnType;
import com.expensetracker.dto.CategoryDtos.CategoryRequest;
import com.expensetracker.dto.CategoryDtos.CategoryResponse;
import com.expensetracker.dto.CategoryDtos.DeleteResult;
import com.expensetracker.exception.BadRequestException;
import com.expensetracker.exception.ConflictException;
import com.expensetracker.exception.NotFoundException;
import com.expensetracker.repository.BudgetRepository;
import com.expensetracker.repository.CategoryRepository;
import com.expensetracker.repository.RecurringExpenseRepository;
import com.expensetracker.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class CategoryService {

    // Keep in step with db/migration/V2__more_default_categories.sql, which adds these for existing users.
    private static final List<Category> DEFAULTS = List.of(
            new Category(null, "🍔 Food & Dining", TxnType.EXPENSE, "#f97316"),
            new Category(null, "🛒 Groceries", TxnType.EXPENSE, "#16a34a"),
            new Category(null, "🚗 Transportation", TxnType.EXPENSE, "#ea580c"),
            new Category(null, "🏠 Rent & Housing", TxnType.EXPENSE, "#7c3aed"),
            new Category(null, "💡 Utilities & Bills", TxnType.EXPENSE, "#0891b2"),
            new Category(null, "📱 Mobile & Recharge", TxnType.EXPENSE, "#0284c7"),
            new Category(null, "🛍️ Shopping", TxnType.EXPENSE, "#ca8a04"),
            new Category(null, "☕ Tea & Coffee", TxnType.EXPENSE, "#92400e"),
            new Category(null, "🏥 Healthcare", TxnType.EXPENSE, "#dc2626"),
            new Category(null, "🎬 Entertainment", TxnType.EXPENSE, "#db2777"),
            new Category(null, "🎓 Education", TxnType.EXPENSE, "#2563eb"),
            new Category(null, "💳 EMIs & Loans", TxnType.EXPENSE, "#be123c"),
            new Category(null, "👨‍👩‍👧 Family & Personal", TxnType.EXPENSE, "#c026d3"),
            new Category(null, "✈️ Travel", TxnType.EXPENSE, "#0d9488"),
            new Category(null, "💰 Savings & Investments", TxnType.EXPENSE, "#65a30d"),
            new Category(null, "📦 Miscellaneous", TxnType.EXPENSE, "#64748b"),
            new Category(null, "Salary", TxnType.INCOME, "#059669"),
            new Category(null, "Business", TxnType.INCOME, "#0d9488"),
            new Category(null, "Gifts", TxnType.INCOME, "#9333ea"),
            new Category(null, "Other", TxnType.INCOME, "#475569"));

    private final CategoryRepository categories;
    private final TransactionRepository transactions;
    private final BudgetRepository budgets;
    private final RecurringExpenseRepository recurring;

    public CategoryService(CategoryRepository categories, TransactionRepository transactions,
                           BudgetRepository budgets, RecurringExpenseRepository recurring) {
        this.categories = categories;
        this.transactions = transactions;
        this.budgets = budgets;
        this.recurring = recurring;
    }

    @Transactional
    public void createDefaults(Long userId) {
        categories.saveAll(DEFAULTS.stream()
                .map(d -> new Category(userId, d.getName(), d.getType(), d.getColor()))
                .toList());
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> list(Long userId, TxnType type, boolean includeArchived) {
        return categories.findByUserIdOrderByTypeAscNameAsc(userId).stream()
                .filter(c -> type == null || c.getType() == type)
                .filter(c -> includeArchived || !c.isArchived())
                .sorted(Comparator.comparing((Category c) -> c.getType().name()).thenComparing(c -> sortKey(c.getName())))
                .map(CategoryResponse::of)
                .toList();
    }

    @Transactional
    public CategoryResponse create(Long userId, CategoryRequest req) {
        String name = req.name().trim();
        if (categories.existsByUserIdAndTypeAndNameIgnoreCase(userId, req.type(), name)) {
            throw new ConflictException("You already have a category with this name");
        }
        return CategoryResponse.of(categories.save(new Category(userId, name, req.type(), req.color())));
    }

    @Transactional
    public CategoryResponse update(Long userId, Long id, CategoryRequest req) {
        Category c = getOwned(userId, id);
        if (req.type() != c.getType()) {
            throw new BadRequestException("A category's type can't be changed");
        }
        String name = req.name().trim();
        if (categories.existsByUserIdAndTypeAndNameIgnoreCaseAndIdNot(userId, c.getType(), name, id)) {
            throw new ConflictException("You already have a category with this name");
        }
        c.setName(name);
        c.setColor(req.color());
        c.setArchived(false);
        return CategoryResponse.of(c);
    }

    /** Deletes an unused category; archives one that transactions, budgets or recurring rules still use. */
    @Transactional
    public DeleteResult delete(Long userId, Long id) {
        Category c = getOwned(userId, id);
        boolean inUse = transactions.existsByCategoryId(id) || budgets.existsByCategoryId(id)
                || recurring.existsByCategoryId(id);
        if (inUse) {
            c.setArchived(true);
            return new DeleteResult(false, true);
        }
        categories.delete(c);
        return new DeleteResult(true, false);
    }

    /** Looks up a category owned by the user; other users' categories are reported as not found. */
    @Transactional(readOnly = true)
    public Category getOwned(Long userId, Long id) {
        return categories.findByIdAndUserId(id, userId).orElseThrow(() -> new NotFoundException("Category"));
    }

    /** Like {@link #getOwned} but also rejects archived categories for new records. */
    @Transactional(readOnly = true)
    public Category getUsable(Long userId, Long id) {
        Category c = getOwned(userId, id);
        if (c.isArchived()) {
            throw new BadRequestException("This category is archived");
        }
        return c;
    }

    /** Sorts by the words, so a leading emoji such as "🛒 " doesn't decide the order; catch-alls go last. */
    static String sortKey(String name) {
        String key = name.replaceFirst("^[^\\p{L}\\p{N}]+", "").toLowerCase(Locale.ROOT);
        return key.equals("other") || key.equals("miscellaneous") ? "￿" + key : key;
    }
}
