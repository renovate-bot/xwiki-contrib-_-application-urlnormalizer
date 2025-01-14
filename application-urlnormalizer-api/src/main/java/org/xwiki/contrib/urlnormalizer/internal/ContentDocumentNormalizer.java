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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.urlnormalizer.DocumentNormalizer;
import org.xwiki.contrib.urlnormalizer.NormalizationException;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.BlockRenderer;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Normalizer for the document content.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Named(ContentDocumentNormalizer.HINT)
@Singleton
public class ContentDocumentNormalizer implements DocumentNormalizer
{
    /**
     * The hint of this component.
     */
    public static final String HINT = "content";

    @Inject
    @Named("link")
    private XDOMNormalizer linkXDOMNormalizer;

    @Inject
    @Named("image")
    private XDOMNormalizer imageXDOMNormalizer;

    @Inject
    @Named("macro")
    private XDOMNormalizer macroXDOMNormalizer;

    @Override
    public boolean normalize(XWikiDocument document, Parser parser, BlockRenderer blockRenderer)
        throws NormalizationException
    {
        XDOM xdom = document.getXDOM();

        boolean modified = this.linkXDOMNormalizer.normalize(xdom, parser, blockRenderer);
        modified |= this.imageXDOMNormalizer.normalize(xdom, parser, blockRenderer);
        modified |= this.macroXDOMNormalizer.normalize(xdom, parser, blockRenderer);

        if (modified) {
            try {
                document.setContent(xdom);
            } catch (XWikiException e) {
                throw new NormalizationException("Failed to normalize the document content.", e);
            }
        }

        return modified;
    }
}
