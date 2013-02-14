/**
 * Copyright 2012 International Business Machines Corp.
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jsr352.tck.chunkartifacts;

import jsr352.tck.chunktypes.InventoryRecord;


@javax.inject.Named("inventoryProcessor")
public class InventoryProcessor implements javax.batch.api.ItemProcessor<InventoryRecord, InventoryRecord>{

	
	@Override
	public InventoryRecord processItem(InventoryRecord record) throws Exception {
		

	    //The processor doesn't really do anything in this test. It just passes along 
	    //the item item and the quantity to the item writer to create and order in the 
	    //order table.
	    
		int itemID = record.getItemID();
		int quantity = record.getQuantity();
		
		return new InventoryRecord(itemID, quantity);
	}
	
}
