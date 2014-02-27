package org.wso2.balana.openaz.test;

import java.util.Date;
import java.util.Iterator;

import org.openliberty.openaz.azapi.AzAttribute;
import org.openliberty.openaz.azapi.AzEntity;
import org.openliberty.openaz.azapi.AzRequestContext;
import org.openliberty.openaz.azapi.AzResponseContext;
import org.openliberty.openaz.azapi.AzResult;
import org.openliberty.openaz.azapi.AzService;
import org.openliberty.openaz.azapi.constants.AzCategoryIdAction;
import org.openliberty.openaz.azapi.constants.AzCategoryIdEnvironment;
import org.openliberty.openaz.azapi.constants.AzCategoryIdResource;
import org.openliberty.openaz.azapi.constants.AzCategoryIdSubjectAccess;
import org.openliberty.openaz.azapi.constants.AzDataTypeIdDateTime;
import org.openliberty.openaz.azapi.constants.AzDataTypeIdString;
import org.openliberty.openaz.azapi.constants.AzXacmlStrings;
import org.wso2.balana.openaz.provider.AzServiceFactory;

public class HelloWorld {

	public static void main(String[] args) {
		
		AzService azHandle = AzServiceFactory.getAzService(); 
		 
		 
		 AzRequestContext azReqCtx = azHandle.createAzRequestContext(); 
		 
		 
		 AzEntity<AzCategoryIdSubjectAccess> accSubj = 
		 azReqCtx.createAzEntity( AzCategoryIdSubjectAccess.AZ_CATEGORY_ID_SUBJECT_ACCESS);
		 
		 
		 accSubj.createAzAttribute( "AuthNSystem", AzXacmlStrings.X_ATTR_SUBJECT_ID ,
		
		accSubj.createAzAttributeValue(AzDataTypeIdString.AZ_DATATYPE_ID_STRING, "alice"));
		 
		 azReqCtx.addAzEntity(accSubj); 
		 
		 
		 AzEntity<AzCategoryIdResource> azResource = 
		 azReqCtx.createAzEntity(AzCategoryIdResource.AZ_CATEGORY_ID_RESOURCE); 
		 
		 azResource.createAzAttribute("applicationName", AzXacmlStrings.X_ATTR_RESOURCE_ID,
		 azResource.createAzAttributeValue(AzDataTypeIdString.AZ_DATATYPE_ID_STRING, "file:/home/rajitha/Desktop/t.txt"));
		 
		 azReqCtx.addAzEntity(azResource); 
		 
		 
		 AzEntity<AzCategoryIdAction> azAction = 
		azReqCtx.createAzEntity(AzCategoryIdAction.AZ_CATEGORY_ID_ACTION);
		 
		 azAction.createAzAttribute("applicationName", AzXacmlStrings.X_ATTR_ACTION_ID,
		 azAction.createAzAttributeValue(AzDataTypeIdString.AZ_DATATYPE_ID_STRING, "Read"));
		 
		 
		 azReqCtx.addAzEntity(azAction); 
		 
		 AzEntity<AzCategoryIdEnvironment> azEnv = 
		 azReqCtx.createAzEntity(AzCategoryIdEnvironment.AZ_CATEGORY_ID_ENVIRONMENT);
		 
		 
		 AzAttribute<AzCategoryIdEnvironment> azEnvAttr = 
		 azEnv.createAzAttribute("applicationName", null, 
		 azEnv.createAzAttributeValue(AzDataTypeIdDateTime.AZ_DATATYPE_ID_DATETIME, 
		 azEnv.createAzDataDateTime(new Date(),0,0,0)));
		 
		 azReqCtx.addAzEntity(azEnv); 
		 
		 AzResponseContext azRspCtx = azHandle.decide(azReqCtx); 
		 
		 AzResult azResult = null;
		 
		Iterator<AzResult> itResults = azRspCtx.getResults().iterator();
		 
		if (itResults.hasNext()) { 
		azResult = itResults.next(); 
		System.out.println(azResult.getAzDecision());
		}

	}
}
