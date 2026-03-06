#!/usr/bin/env python3
"""
Script to convert old backup format to new format compatible with current app.
Old format: { "version": "...", "timestamp": "...", "data": { ... } }
New format: BackupData structure (flat with underscore_to_camel_case conversion)
"""

import json
import sys

def convert_backup(input_file, output_file):
    # Read old backup file
    with open(input_file, 'r') as f:
        old_data = json.load(f)
    
    # Extract data from old format
    if 'data' not in old_data:
        print("Error: No 'data' field found in backup file")
        sys.exit(1)
    
    data = old_data['data']
    
    # Convert snake_case to camelCase for compatibility with Kotlin data classes
    new_data = {}
    
    # Map old field names to new field names
    field_mapping = {
        'products': 'products',
        'customers': 'customers',
        'payment_methods': 'paymentMethods',
        'shop_profile': 'shopProfile',
        'sales': 'sales',
        'sales_items': 'saleItems',
        'suppliers': 'suppliers',
        'purchases': 'purchases',
        'purchase_items': 'purchaseItems',
        'receivables': 'receivables',
        'payables': 'payables',
        'phone_history': 'phoneHistory',
        'digital_products': 'digitalProducts',
        'digital_categories': 'digitalCategories',
        'expenses': 'expenses',
        'customer_points_history': 'customerPointsHistory',
        'stock_history': 'stockHistory'
    }
    
    for old_key, new_key in field_mapping.items():
        if old_key in data:
            value = data[old_key]
            # Handle shop_profile which is an array in old format but single entity in new format
            if old_key == 'shop_profile' and isinstance(value, list):
                value = value[0] if value else None
            new_data[new_key] = value
    
    # Write new format
    with open(output_file, 'w') as f:
        json.dump(new_data, f, indent=2)
    
    print(f"Successfully converted {input_file} to {output_file}")
    print(f"Fields converted: {list(new_data.keys())}")

if __name__ == '__main__':
    if len(sys.argv) != 3:
        print("Usage: python convert_backup.py <input_file> <output_file>")
        sys.exit(1)
    
    convert_backup(sys.argv[1], sys.argv[2])
