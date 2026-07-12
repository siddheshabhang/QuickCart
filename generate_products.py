import json
import random

brand_mapping = {
    "Dairy & Bakery": ["Amul", "Mother Dairy", "Britannia", "Gowardhan"],
    "Snacks & Beverages": ["Lays", "Haldiram", "Balaji", "Coca Cola", "Pepsi", "Parle"],
    "Fruits & Vegetables": ["Fresh", "Local Farmer", "Organic Farm"],
    "Staples": ["Aashirvaad", "Fortune", "Tata", "India Gate"],
    "Personal Care": ["Dove", "Nivea", "Gillette", "Colgate", "Himalaya"]
}

items_mapping = {
    "Dairy & Bakery": ["Milk", "Cheese", "Butter", "Paneer", "Yogurt", "Bread", "Pav", "Croissant", "Eggs"],
    "Snacks & Beverages": ["Chips", "Biscuits", "Namkeen", "Cold Drink", "Juice", "Tea", "Coffee", "Energy Drink"],
    "Fruits & Vegetables": ["Apple", "Banana", "Onion", "Tomato", "Potato", "Spinach", "Mango", "Grapes", "Lemon"],
    "Staples": ["Rice", "Wheat Flour", "Toor Dal", "Moong Dal", "Sugar", "Salt", "Cooking Oil", "Ghee"],
    "Personal Care": ["Soap", "Shampoo", "Toothpaste", "Deodorant", "Face Wash", "Body Lotion", "Hair Oil"]
}

products = []

while len(products) < 100:
    cat_name = random.choice(list(items_mapping.keys()))
    item = random.choice(items_mapping[cat_name])
    brand = random.choice(brand_mapping[cat_name])
    
    name = f"{brand} {item}"
    if name not in [p["name"] for p in products]:
        price = round(random.uniform(10.0, 300.0), 2)
        desc = f"High quality {item.lower()} from {brand}."
        products.append({
            "name": name,
            "price": price,
            "description": desc
        })

with open("product-service/src/main/resources/products.json", "w") as f:
    json.dump(products, f, indent=4)

print("Generated accurate products.json")
