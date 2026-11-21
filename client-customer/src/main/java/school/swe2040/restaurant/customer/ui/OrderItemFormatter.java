package school.swe2040.restaurant.customer.ui;

import school.swe2040.restaurant.shared.model.MenuCategory;
import school.swe2040.restaurant.shared.model.OrderItem;
import school.swe2040.restaurant.shared.model.Size;
import school.swe2040.restaurant.shared.model.SpiceLevel;

import java.util.ArrayList;
import java.util.List;

/**
 * Small, UI-only text and icon helpers shared by the menu cards, the cart
 * list, the customize dialog and the order-status cards, so icon choices
 * and label wording for categories, sizes and spice levels stay consistent
 * in one place instead of being duplicated per class. Pure presentation:
 * it only reads existing getters on the shared model, it does not add to
 * or change that model.
 */
final class OrderItemFormatter {

    private OrderItemFormatter() {
    }

    /** A single Unicode glyph used as a lightweight, dependency-free icon for a menu category. */
    static String categoryIcon(MenuCategory category) {
        switch (category) {
            case STARTER:
                return "🥗"; // salad
            case MAIN:
                return "🍽"; // fork, knife and plate
            case DRINK:
                return "🥤"; // cup with straw
            case DESSERT:
                return "🍰"; // cake slice
            default:
                return "•";
        }
    }

    static String sizeLabel(Size size) {
        if (size == null) {
            return "";
        }
        switch (size) {
            case SMALL:
                return "Small";
            case REGULAR:
                return "Regular";
            case LARGE:
                return "Large";
            default:
                return capitalize(size.name());
        }
    }

    static String spiceLabel(SpiceLevel level) {
        if (level == null) {
            return "";
        }
        switch (level) {
            case MILD:
                return "Mild";
            case MEDIUM:
                return "Medium";
            case HOT:
                return "Hot";
            case EXTRA_HOT:
                return "Extra Hot";
            default:
                return capitalize(level.name());
        }
    }

    /** One chili pepper glyph per spice level: mild is one, extra hot is four. */
    static String spiceIcon(SpiceLevel level) {
        if (level == null) {
            return "";
        }
        StringBuilder icon = new StringBuilder();
        for (int i = 0; i <= level.ordinal(); i++) {
            icon.append("🌶"); // hot pepper
        }
        return icon.toString();
    }

    /**
     * A short, human-readable summary of the customizations on {@code item}
     * (size, spice level, add-ons, note), for display in the cart and on
     * order-status cards. Empty if nothing beyond the plain base item was
     * chosen, so a plain order does not show a clutter of empty fields.
     */
    static String customizationSummary(OrderItem item) {
        List<String> parts = new ArrayList<>();
        if (item.getSize() != null && item.getSize() != Size.REGULAR) {
            parts.add(sizeLabel(item.getSize()));
        }
        if (item.getSpiceLevel() != null) {
            parts.add(spiceIcon(item.getSpiceLevel()) + " " + spiceLabel(item.getSpiceLevel()));
        }
        if (!item.getSelectedAddOns().isEmpty()) {
            parts.add("+" + String.join(", ", item.getSelectedAddOns()));
        }
        if (item.getNote() != null && !item.getNote().trim().isEmpty()) {
            parts.add("Note: " + item.getNote().trim());
        }
        return String.join("   ·   ", parts);
    }

    static String capitalize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String lower = text.toLowerCase().replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
