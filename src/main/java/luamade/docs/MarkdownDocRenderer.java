package luamade.docs;

import org.commonmark.node.*;
import org.commonmark.parser.Parser;

import java.util.*;

public final class MarkdownDocRenderer {

	private static final Parser PARSER = Parser.builder().build();

	private MarkdownDocRenderer() {
	}

	public static List<RenderedBlock> render(String markdown) {
		NodeCollector collector = new NodeCollector();
		Node document = PARSER.parse(markdown == null ? "" : markdown);
		document.accept(collector);
		return collector.getBlocks();
	}

	private static String normalize(String text) {
		if(text == null) {
			return "";
		}
		return text.replace("\r", "").trim();
	}

	public enum BlockType {
		HEADING_1,
		HEADING_2,
		HEADING_3,
		PARAGRAPH,
		BULLET,
		ORDERED,
		CODE,
		SEPARATOR
	}

	private static String flattenSegments(List<InlineSegment> segments) {
		StringBuilder builder = new StringBuilder();
		for(InlineSegment segment : segments) {
			builder.append(segment.getText());
		}
		return builder.toString();
	}

	public enum InlineStyle {
		NORMAL,
		BOLD,
		ITALIC,
		BOLD_ITALIC,
		INLINE_CODE
	}

	public static final class InlineSegment {
		private final InlineStyle style;
		private final String text;

		public InlineSegment(InlineStyle style, String text) {
			this.style = style;
			this.text = text;
		}

		public InlineStyle getStyle() {
			return style;
		}

		public String getText() {
			return text;
		}
	}

	public static final class RenderedBlock {
		private final BlockType type;
		private final String text;
		private final List<InlineSegment> segments;

		public RenderedBlock(BlockType type, String text) {
			this(type, text, Collections.singletonList(new InlineSegment(InlineStyle.NORMAL, text)));
		}

		public RenderedBlock(BlockType type, String text, List<InlineSegment> segments) {
			this.type = type;
			this.text = text;
			this.segments = Collections.unmodifiableList(new ArrayList<>(segments));
		}

		public BlockType getType() {
			return type;
		}

		public String getText() {
			return text;
		}

		public List<InlineSegment> getSegments() {
			return segments;
		}
	}

	private static final class NodeCollector extends AbstractVisitor {

		private final List<RenderedBlock> blocks = new ArrayList<>();
		private int orderedListCounter = 1;

		public List<RenderedBlock> getBlocks() {
			return blocks;
		}

		@Override
		public void visit(Heading heading) {
			List<InlineSegment> segments = InlineCollector.collect(heading, null);
			String text = normalize(flattenSegments(segments));
			if(text.isEmpty()) {
				return;
			}
			switch(Math.min(heading.getLevel(), 3)) {
				case 1:
					blocks.add(new RenderedBlock(BlockType.HEADING_1, text, segments));
					break;
				case 2:
					blocks.add(new RenderedBlock(BlockType.HEADING_2, text, segments));
					break;
				default:
					blocks.add(new RenderedBlock(BlockType.HEADING_3, text, segments));
					break;
			}
		}

		@Override
		public void visit(Paragraph paragraph) {
			if(paragraph.getParent() instanceof ListItem) {
				return;
			}
			List<InlineSegment> segments = InlineCollector.collect(paragraph, null);
			String text = normalize(flattenSegments(segments));
			if(!text.isEmpty()) {
				blocks.add(new RenderedBlock(BlockType.PARAGRAPH, text, segments));
			}
		}

		@Override
		public void visit(BulletList bulletList) {
			visitChildren(bulletList);
		}

		@Override
		public void visit(OrderedList orderedList) {
			int previous = orderedListCounter;
			orderedListCounter = orderedList.getStartNumber();
			visitChildren(orderedList);
			orderedListCounter = previous;
		}

		@Override
		public void visit(ListItem listItem) {
			String prefix;
			BlockType type;
			if(listItem.getParent() instanceof OrderedList) {
				prefix = orderedListCounter + ". ";
				type = BlockType.ORDERED;
			} else {
				prefix = "• ";
				type = BlockType.BULLET;
			}

			List<InlineSegment> segments = InlineCollector.collect(listItem, prefix);
			String text = normalize(flattenSegments(segments));
			if(text.isEmpty()) {
				return;
			}
			if(listItem.getParent() instanceof OrderedList) {
				blocks.add(new RenderedBlock(type, text, segments));
				orderedListCounter++;
			} else {
				blocks.add(new RenderedBlock(type, text, segments));
			}
		}

		@Override
		public void visit(FencedCodeBlock fencedCodeBlock) {
			String text = fencedCodeBlock.getLiteral() == null ? "" : fencedCodeBlock.getLiteral().trim();
			if(!text.isEmpty()) {
				blocks.add(new RenderedBlock(BlockType.CODE, text));
			}
		}

		@Override
		public void visit(IndentedCodeBlock indentedCodeBlock) {
			String text = indentedCodeBlock.getLiteral() == null ? "" : indentedCodeBlock.getLiteral().trim();
			if(!text.isEmpty()) {
				blocks.add(new RenderedBlock(BlockType.CODE, text));
			}
		}

		@Override
		public void visit(ThematicBreak thematicBreak) {
			blocks.add(new RenderedBlock(BlockType.SEPARATOR, "────────────────────────────────"));
		}
	}

	private static final class InlineCollector extends AbstractVisitor {

		private final List<InlineSegment> segments = new ArrayList<>();
		private final Deque<Boolean> boldStack = new ArrayDeque<>();
		private final Deque<Boolean> italicStack = new ArrayDeque<>();

		private InlineCollector() {
			boldStack.push(Boolean.FALSE);
			italicStack.push(Boolean.FALSE);
		}

		public static List<InlineSegment> collect(Node node, String prefix) {
			InlineCollector collector = new InlineCollector();
			if(prefix != null && !prefix.isEmpty()) {
				collector.append(InlineStyle.NORMAL, prefix);
			}
			node.accept(collector);
			return collector.segments;
		}

		@Override
		public void visit(Text text) {
			append(resolveStyle(), text.getLiteral());
		}

		@Override
		public void visit(Code code) {
			append(InlineStyle.INLINE_CODE, code.getLiteral());
		}

		@Override
		public void visit(SoftLineBreak softLineBreak) {
			append(resolveStyle(), " ");
		}

		@Override
		public void visit(HardLineBreak hardLineBreak) {
			append(resolveStyle(), "\n");
		}

		@Override
		public void visit(Emphasis emphasis) {
			italicStack.push(Boolean.TRUE);
			visitChildren(emphasis);
			italicStack.pop();
		}

		@Override
		public void visit(StrongEmphasis strongEmphasis) {
			boldStack.push(Boolean.TRUE);
			visitChildren(strongEmphasis);
			boldStack.pop();
		}

		@Override
		public void visit(Link link) {
			visitChildren(link);
		}

		@Override
		public void visit(Image image) {
			visitChildren(image);
		}

		private void append(InlineStyle style, String text) {
			String normalizedText = text == null ? "" : text.replace("\r", "");
			if(normalizedText.isEmpty()) {
				return;
			}

			InlineSegment previous = segments.isEmpty() ? null : segments.get(segments.size() - 1);
			if(previous != null && previous.getStyle() == style) {
				segments.set(segments.size() - 1, new InlineSegment(style, previous.getText() + normalizedText));
				return;
			}

			segments.add(new InlineSegment(style, normalizedText));
		}

		private InlineStyle resolveStyle() {
			boolean bold = !boldStack.isEmpty() && boldStack.peek();
			boolean italic = !italicStack.isEmpty() && italicStack.peek();
			if(bold && italic) {
				return InlineStyle.BOLD_ITALIC;
			}
			if(bold) {
				return InlineStyle.BOLD;
			}
			if(italic) {
				return InlineStyle.ITALIC;
			}
			return InlineStyle.NORMAL;
		}
	}
}

