package prj.fschnabel.idea.linkfilter;

import com.intellij.execution.filters.*;
import com.intellij.ide.browsers.OpenUrlHyperlinkInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OutputLinkFilter implements Filter {
	
	private static final Pattern FILE_PATTERN = Pattern.compile(
			"\\b((/|([a-zA-Z]:[\\\\/]))[a-zA-Z0-9/\\\\\\-_. ]+)(:(\\d+))?(:(\\d+))?"
	);
	
	private final Project project;
	
	public OutputLinkFilter(Project project) {
		this.project = project;
	}
	
	@Override
	public Result applyFilter(String line, int entireLength) {
		int textStartOffset = entireLength - line.length();
		Matcher m = FILE_PATTERN.matcher(line);
		ResultItem item = null;
		List<ResultItem> items = null;
		while (m.find()) {
			if (item == null) {
				item = new ResultItem(textStartOffset + m.start(), textStartOffset + m.end(), buildHyperlinkInfo(m.group()));
			} else {
				if (items == null) {
					items = new ArrayList<>(2);
					items.add(item);
				}
				items.add(new ResultItem(textStartOffset + m.start(), textStartOffset + m.end(), buildHyperlinkInfo(m.group())));
			}
		}
		if (items != null) return new Result(items);
		if (item != null)
			return new Result(item.getHighlightStartOffset(), item.getHighlightEndOffset(), item.getHyperlinkInfo());
		return null;
	}
	
	/**
	 * Copied from {@link UrlFilter#buildHyperlinkInfo(String) }
	 */
	protected HyperlinkInfo buildHyperlinkInfo(String url) {
		HyperlinkInfo fileHyperlinkInfo = buildFileHyperlinkInfo(url);
		return fileHyperlinkInfo != null ? fileHyperlinkInfo : new OpenUrlHyperlinkInfo(url);
	}
	
	/**
	 * Copied from {@link UrlFilter#buildFileHyperlinkInfo(String)}
	 */
	private @Nullable HyperlinkInfo buildFileHyperlinkInfo(String url) {
		if (url.endsWith(".html") || url.startsWith("file://")) {
			return null;
		}
		int documentLine = 0;
		int documentColumn = 0;
		int filePathEndIndex = url.length();
		final int lastColonInd = url.lastIndexOf(':');
		if (lastColonInd < url.length() - 1) {
			int lastValue = StringUtil.parseInt(url.substring(lastColonInd + 1), Integer.MIN_VALUE);
			if (lastValue != Integer.MIN_VALUE) {
				documentLine = lastValue - 1;
				filePathEndIndex = lastColonInd;
				int preLastColonInd = url.lastIndexOf(':', lastColonInd - 1);
				int preLastValue = StringUtil.parseInt(url.substring(preLastColonInd + 1, lastColonInd), Integer.MIN_VALUE);
				if (preLastValue != Integer.MIN_VALUE) {
					documentLine = preLastValue - 1;
					documentColumn = lastValue - 1;
					filePathEndIndex = preLastColonInd;
				}
				
			}
		}
		String filePath = url.substring(0, filePathEndIndex);
		return new UrlFilter.FileUrlHyperlinkInfo(project, filePath, documentLine, documentColumn, url, true);
	}
}