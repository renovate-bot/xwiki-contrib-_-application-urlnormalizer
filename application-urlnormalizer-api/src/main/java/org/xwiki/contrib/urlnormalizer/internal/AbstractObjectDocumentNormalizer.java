/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.contrib.urlnormalizer.internal;

import java.io.StringReader;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.contrib.urlnormalizer.DocumentNormalizer;
import org.xwiki.contrib.urlnormalizer.NormalizationException;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;

/**
 * Abstract for object document normalizers. These normalizers only look for TextArea properties within the XWiki
 * documents.
 *
 * @version $Id$
 * @since 1.4
 */
public abstract class AbstractObjectDocumentNormalizer implements DocumentNormalizer
{
    protected static final String TEXT_AREA = "TextArea";

    @Inject
    @Named("link")
    protected XDOMNormalizer linkXDOMNormalizer;

    @Inject
    @Named("image")
    protected XDOMNormalizer imageXDOMNormalizer;

    @Inject
    @Named("macro")
    protected XDOMNormalizer macroXDOMNormalizer;

    @Inject
    protected Provider<XWikiContext> xcontextProvider;

    /**
     * Normalize the given XObject property.
     *
     * @param property the property to normalize
     * @param parser the parser to use
     * @param blockRenderer the block renderer to use
     * @return true if the object has been modified, false otherwise
     * @throws NormalizationException if an error happens
     */
    protected boolean normalize(BaseProperty<?> property, Parser parser, BlockRenderer blockRenderer)
        throws NormalizationException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        PropertyClass propertyClass = property.getPropertyClass(xcontext);

        // Only handle textarea properties with wiki content
        if (propertyClass instanceof TextAreaClass && ((TextAreaClass) propertyClass).isWikiContent()) {
            String content = (String) property.getValue();

            try {
                XDOM xdom = parser.parse(new StringReader(content));

                boolean modified = this.linkXDOMNormalizer.normalize(xdom, parser, blockRenderer);
                modified |= this.imageXDOMNormalizer.normalize(xdom, parser, blockRenderer);
                modified |= this.macroXDOMNormalizer.normalize(xdom, parser, blockRenderer);

                if (modified) {
                    WikiPrinter wikiPrinter = new DefaultWikiPrinter();
                    blockRenderer.render(xdom, wikiPrinter);
                    String normalizedContent = wikiPrinter.toString();
                    property.setValue(normalizedContent);
                }

                return modified;
            } catch (ParseException e) {
                // The parser for the syntax of the document may not fit the syntax used in a XProperty.
                throw new NormalizationException(
                    String.format("Failed to normalize URLs in TextArea property [%s]", property.getReference()), e);
            }
        }

        return false;
    }
}
