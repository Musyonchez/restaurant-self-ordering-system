package school.swe2040.restaurant.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * The restaurant's fixed menu. This is the single source of truth for menu
 * items: both the customer client and the server use this class, so an item
 * id always means the same name, price and customization options on both
 * ends of the socket connection.
 */
public final class MenuCatalog {

    private static final List<MenuItem> ITEMS = Collections.unmodifiableList(buildMenu());

    private MenuCatalog() {
    }

    public static List<MenuItem> getAllItems() {
        return ITEMS;
    }

    public static List<MenuItem> getItemsByCategory(MenuCategory category) {
        List<MenuItem> result = new ArrayList<>();
        for (MenuItem item : ITEMS) {
            if (item.getCategory() == category) {
                result.add(item);
            }
        }
        return result;
    }

    public static Optional<MenuItem> findById(String id) {
        return ITEMS.stream().filter(item -> item.getId().equals(id)).findFirst();
    }

    private static List<MenuItem> buildMenu() {
        List<MenuItem> items = new ArrayList<>();

        items.add(new MenuItem("starter-bruschetta", "Bruschetta",
                "Grilled sourdough, vine tomatoes, basil, olive oil",
                MenuCategory.STARTER, 6.50, false, false, Collections.emptyList()));
        items.add(new MenuItem("starter-calamari", "Crispy Calamari",
                "Lightly fried squid, lemon aioli",
                MenuCategory.STARTER, 8.00, false, true, Arrays.asList("Extra lemon aioli", "Chili flakes")));
        items.add(new MenuItem("starter-soup", "Roasted Tomato Soup",
                "Slow-roasted tomato and basil soup with a cream swirl",
                MenuCategory.STARTER, 5.50, true, false, Collections.singletonList("Garlic croutons")));

        items.add(new MenuItem("main-steak", "Grilled Sirloin Steak",
                "8oz sirloin, herb butter, served with your choice of sides",
                MenuCategory.MAIN, 22.00, true, true, Arrays.asList("Extra herb butter", "Grilled mushrooms", "Onion rings")));
        items.add(new MenuItem("main-salmon", "Pan-Seared Salmon",
                "Salmon fillet, lemon butter sauce, seasonal vegetables",
                MenuCategory.MAIN, 19.50, true, false, Arrays.asList("Extra lemon butter sauce", "Steamed asparagus")));
        items.add(new MenuItem("main-pasta", "Spicy Arrabbiata Pasta",
                "Penne in a spicy tomato and garlic sauce",
                MenuCategory.MAIN, 14.50, true, true, Arrays.asList("Parmesan shavings", "Grilled chicken")));
        items.add(new MenuItem("main-burger", "House Burger",
                "Beef patty, cheddar, caramelized onions, brioche bun, fries",
                MenuCategory.MAIN, 15.00, false, true, Arrays.asList("Extra cheese", "Bacon", "Fried egg")));
        items.add(new MenuItem("main-risotto", "Mushroom Risotto",
                "Creamy arborio rice, wild mushrooms, truffle oil",
                MenuCategory.MAIN, 16.50, false, false, Collections.singletonList("Extra truffle oil")));

        items.add(new MenuItem("drink-water", "Sparkling Water",
                "Bottled sparkling mineral water",
                MenuCategory.DRINK, 3.00, true, false, Collections.emptyList()));
        items.add(new MenuItem("drink-soda", "Soft Drink",
                "Cola, lemonade or orange soda",
                MenuCategory.DRINK, 3.50, true, false, Collections.emptyList()));
        items.add(new MenuItem("drink-juice", "Fresh Orange Juice",
                "Freshly squeezed orange juice",
                MenuCategory.DRINK, 4.50, true, false, Collections.emptyList()));
        items.add(new MenuItem("drink-coffee", "House Coffee",
                "Freshly brewed coffee",
                MenuCategory.DRINK, 3.80, false, false, Arrays.asList("Extra shot", "Oat milk")));

        items.add(new MenuItem("dessert-cheesecake", "New York Cheesecake",
                "Classic baked cheesecake, berry compote",
                MenuCategory.DESSERT, 7.50, false, false, Collections.singletonList("Extra berry compote")));
        items.add(new MenuItem("dessert-lavacake", "Chocolate Lava Cake",
                "Warm chocolate cake with a molten center, vanilla ice cream",
                MenuCategory.DESSERT, 8.50, false, false, Collections.singletonList("Extra scoop of ice cream")));
        items.add(new MenuItem("dessert-icecream", "Ice Cream Trio",
                "Three scoops: vanilla, chocolate, strawberry",
                MenuCategory.DESSERT, 6.00, false, false, Collections.emptyList()));

        return items;
    }
}
