# Liquibase Composite Primary Key Guide

## Overview
This guide shows how to create composite (multi-column) primary keys in Liquibase XML format.

## Method 1: Using `addPrimaryKey` (Recommended)

When you need a composite primary key, define the columns first (without `primaryKey="true"`), then use `addPrimaryKey` to combine them.

### Example: Composite Primary Key

```xml
<changeSet id="example-composite-pk" author="system">
    <createTable tableName="example_table">
        <!-- Define columns WITHOUT primaryKey="true" -->
        <column name="id" type="BIGINT">
            <constraints nullable="false"/>
        </column>
        <column name="client_order_id" type="VARCHAR(64)">
            <constraints nullable="false"/>
        </column>
        <column name="country_code" type="VARCHAR(8)">
            <constraints nullable="false"/>
        </column>
        <column name="other_column" type="VARCHAR(100)"/>
    </createTable>
    
    <!-- Add composite primary key AFTER table creation -->
    <addPrimaryKey 
        tableName="example_table"
        columnNames="id, client_order_id"
        constraintName="pk_example_table"/>
</changeSet>
```

## Method 2: Using `addUniqueConstraint` (For Unique Constraints, Not PK)

If you need a unique constraint (not a primary key), use `addUniqueConstraint`:

```xml
<changeSet id="example-unique-constraint" author="system">
    <createTable tableName="example_table">
        <column name="id" type="BIGSERIAL">
            <constraints primaryKey="true" nullable="false"/>
        </column>
        <column name="client_order_id" type="VARCHAR(64)">
            <constraints nullable="false"/>
        </column>
        <column name="country_code" type="VARCHAR(8)">
            <constraints nullable="false"/>
        </column>
    </createTable>
    
    <!-- Add unique constraint on multiple columns -->
    <addUniqueConstraint
        tableName="example_table"
        columnNames="client_order_id, country_code"
        constraintName="uq_example_order_country"/>
</changeSet>
```

## Real-World Example: If We Needed Composite PK

If `final_regional_allocation` needed a composite primary key of `(client_order_id, market)` instead of just `id`:

```xml
<changeSet id="009-create-final-regional-allocation-table-composite-pk" author="system">
    <createTable tableName="final_regional_allocation">
        <!-- No single column has primaryKey="true" -->
        <column name="client_order_id" type="VARCHAR(64)">
            <constraints nullable="false"/>
        </column>
        <column name="market" type="VARCHAR(32)">
            <constraints nullable="false"/>
        </column>
        <column name="asia_allocation" type="NUMERIC(20, 4)"/>
        <column name="allocation" type="NUMERIC(20, 4)"/>
        <column name="effective_order" type="NUMERIC(20, 4)"/>
        <column name="pro_rata" type="NUMERIC(7, 4)"/>
        <column name="allocation_amount" type="NUMERIC(20, 4)"/>
        <column name="created_at" type="TIMESTAMP WITH TIME ZONE" defaultValueComputed="NOW()"/>
        <column name="updated_at" type="TIMESTAMP WITH TIME ZONE" defaultValueComputed="NOW()"/>
    </createTable>
    
    <!-- Add composite primary key -->
    <addPrimaryKey 
        tableName="final_regional_allocation"
        columnNames="client_order_id, market"
        constraintName="pk_final_regional_allocation"/>
    
    <addForeignKeyConstraint
        baseTableName="final_regional_allocation"
        baseColumnNames="client_order_id"
        constraintName="fk_final_regional_alloc_client_order"
        referencedTableName="trader_order"
        referencedColumnNames="client_order_id"
        onDelete="NO ACTION"/>
</changeSet>
```

## Key Points

1. **Don't use `primaryKey="true"` on individual columns** when creating a composite PK
2. **Use `addPrimaryKey`** after `createTable` to define the composite key
3. **Column order matters** - the order in `columnNames` should match your business logic
4. **Constraint name** - Always provide a meaningful constraint name
5. **Foreign keys** - One or more columns in a composite PK can be part of a foreign key

## Syntax Reference

```xml
<addPrimaryKey 
    tableName="table_name"
    columnNames="column1, column2, column3"
    constraintName="pk_table_name"/>
```

### Attributes:
- `tableName` (required): Name of the table
- `columnNames` (required): Comma-separated list of column names
- `constraintName` (required): Name for the primary key constraint
- `schemaName` (optional): Schema name if not default
- `tablespace` (optional): Tablespace for the index

## Current Schema Status

In the current schema (`001-init-schema.xml`), all tables use single-column primary keys:
- `trader_order`: `client_order_id` (PK)
- `trader_sub_order`: `id` (PK)
- `regional_allocation`: `client_order_id` (PK)
- `regional_allocation_breakdown`: `id` (PK)
- `client_allocation_breakdown`: `id` (PK)
- `client_allocation_amend_log`: `id` (PK)
- `trader_order_status_audit`: `id` (PK)
- `final_priced_allocation_breakdown`: `id` (PK)
- `final_regional_allocation`: `id` (PK) with unique constraint on `(client_order_id, market)`

The `final_regional_allocation` table has a unique constraint on `(client_order_id, market)` but uses `id` as the primary key, which is the recommended approach for JPA entities.

