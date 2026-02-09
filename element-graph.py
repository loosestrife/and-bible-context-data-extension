import xml.etree.ElementTree as ET
from collections import defaultdict
import graphviz


def extract_xml_graph_data(xml_file):
    """Parses an XML file and extracts tag and edge counts."""
    tree = ET.parse(xml_file)
    root = tree.getroot()

    tag_counts = defaultdict(int)
    edge_counts = defaultdict(int)

    def traverse_and_count(element, parent_tag=None):
        """Recursively traverses the XML tree to count tags and edges."""
        # Strip namespace if present
        short_tag = element.tag.split('}').pop()
        tag_counts[short_tag] += 1
        if parent_tag:
            # Edges represent parent -> child relationships
            edge_counts[(parent_tag, short_tag)] += 1
        
        for child in element:
            traverse_and_count(child, short_tag)

    traverse_and_count(root)
    return tag_counts, edge_counts


# --- Main execution ---
try:
    # Extract data from the XML file
    tag_counts, edge_counts = extract_xml_graph_data('sword/kjv.osis.xml')
    print("Tag Counts:", tag_counts)
    print("Edge Counts:", edge_counts)
    

    # Create a new directed graph using Graphviz
    dot = graphviz.Digraph('XML_Tag_Inclusion_Graph')
    dot.attr('graph', rankdir='TB', labelloc='t', label='XML Tag Inclusion Graph with Counts')
    dot.attr('node', shape='box', style='rounded,filled', fillcolor='lightblue')

    # Add nodes with counts as labels
    for tag, count in tag_counts.items():
        # The label includes the tag name and its count, separated by a newline
        node_label = f"{tag}\n({count})"
        dot.node(tag, node_label)

    # Add edges with counts as labels
    for (parent, child), count in edge_counts.items():
        dot.edge(parent, child, label=str(count))

    # Render the graph to a file (e.g., element_graph.pdf)
    # The `view=True` argument will try to open the generated file automatically
    output_filename = dot.render('element-graph', view=True, format='png', cleanup=True)
    print(f"Graph has been rendered to: {output_filename}")

except FileNotFoundError:
    print("Error: 'kjv.osis.xml' not found. Please ensure the file is in the same directory.")
except Exception as e:
    print(f"An error occurred: {e}")

