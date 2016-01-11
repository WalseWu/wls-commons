/*
 * Copyright (C) 2015 dzyh
 * All rights reserved.
 *
 * $$File: $$
 * $$DateTime: $$
 * $$Author: $$
 * $$Revision: $$
 */

package common.lucene.comp;

import org.apache.lucene.document.Document;

/**
 * @author wls
 */
public interface Indexable
{
	String DESERIALIZE_METHOD = "deserialize";

	Indexable deserialize(Document doc);

	Document serialize();
}
