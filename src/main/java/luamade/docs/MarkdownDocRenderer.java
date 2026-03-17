package luamade.docs;

import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.text.TextContentRenderer;

import java.util.ArrayList;
import java.util.List;

public final class MarkdownDocRenderer {

	private static final Parser PARSER = Parser.builder().build();
	private static final TextContentRenderer TEXT_RENDERER = TextContentRenderer.builder().build();

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

	public static final class RenderedBlock {
		private final BlockType type;
		private final String text;

		public RenderedBlock(BlockType type, String text) {
			this.type = type;
			this.text = text;
		}

		public BlockType getType() {
			return type;
		}

		public String getText() {
			return text;
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
			String text = normalize(TEXT_RENDERER.render(heading));
			if(text.isEmpty()) {
				return;
			}
			switch(Math.min(heading.getLevel(), 3)) {
				case 1:
					blocks.add(new RenderedBlock(BlockType.HEADING_1, text));
					break;
				case 2:
					blocks.add(new RenderedBlock(BlockType.HEADING_2, text));
					break;
				default:
					blocks.add(new RenderedBlock(BlockType.HEADING_3, text));
					break;
			}
		}

		@Override
		public void visit(Paragraph paragraph) {
			if(paragraph.getParent() instanceof ListItem) {
				return;
			}
			String text = normalize(TEXT_RENDERER.render(paragraph));
			if(!text.isEmpty()) {
				blocks.add(new RenderedBlock(BlockType.PARAGRAPH, text));
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
			String text = normalize(TEXT_RENDERER.render(listItem));
			if(text.isEmpty()) {
				return;
			}
			if(listItem.getParent() instanceof OrderedList) {
				blocks.add(new RenderedBlock(BlockType.ORDERED, orderedListCounter + ". " + text));
				orderedListCounter++;
			} else {
				blocks.add(new RenderedBlock(BlockType.BULLET, "• " + text));
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
}

