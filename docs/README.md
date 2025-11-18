# Fixed Income IPO Order Allocation - Documentation

This directory contains comprehensive documentation for the Fixed Income IPO Order Allocation system.

## Files

### Main Documentation
- **CONFLUENCE_DOCUMENTATION.md** - Complete technical documentation in Confluence-compatible format
  - Overview and architecture
  - Database design with ER diagrams
  - Class diagrams (UML)
  - Process flow diagrams
  - Complete API documentation
  - Entity relationships
  - Status transitions

### Diagrams
The `diagrams/` directory contains PlantUML source files for all diagrams:

- **database-erd.puml** - Entity Relationship Diagram for database tables
- **class-diagram.puml** - UML Class Diagram for entity classes
- **service-diagram.puml** - UML Class Diagram for service layer
- **process-flow.puml** - Process flow diagram for IPO order allocation workflow
- **status-transitions.puml** - State machine diagram for status transitions

## How to Use

### For Confluence

1. **Import Markdown**: The `CONFLUENCE_DOCUMENTATION.md` file can be imported directly into Confluence using the Markdown import feature.

2. **Render PlantUML Diagrams**: 
   - Install the PlantUML plugin in Confluence
   - Copy the contents of `.puml` files into PlantUML code blocks
   - Or use the PlantUML macro to reference the files directly

3. **Alternative**: Use online PlantUML renderer (http://www.plantuml.com/plantuml/uml/) to generate images and upload to Confluence.

### For Local Viewing

1. Install PlantUML:
   ```bash
   brew install plantuml  # macOS
   # or
   apt-get install plantuml  # Linux
   ```

2. Generate diagrams:
   ```bash
   plantuml diagrams/*.puml
   ```

3. View the generated PNG/SVG files in any image viewer.

## Documentation Structure

The documentation covers:

1. **Overview** - Project description and key features
2. **Architecture** - System architecture and package structure
3. **Database Design** - ER diagrams and table descriptions
4. **Class Diagram** - UML diagrams for entities and services
5. **Process Flow** - Workflow diagrams for the allocation process
6. **API Documentation** - Complete REST API reference
7. **Entity Relationships** - Detailed relationship mappings
8. **Status Transitions** - State machine for order statuses

## Updating Documentation

When updating the codebase:

1. Update the relevant sections in `CONFLUENCE_DOCUMENTATION.md`
2. Update PlantUML diagrams if entity structures change
3. Regenerate diagrams if using local PlantUML installation
4. Update version and last modified date in the documentation

## Notes

- All diagrams use PlantUML syntax which is widely supported
- The documentation is written in Markdown for easy conversion to Confluence
- API examples use JSON format
- Database schema reflects the Liquibase changelog structure

