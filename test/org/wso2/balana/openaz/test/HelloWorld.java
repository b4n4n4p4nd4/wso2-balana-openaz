package org.wso2.balana.openaz.test;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

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
import org.wso2.balana.ParsingException;
import org.wso2.balana.UnknownIdentifierException;
import org.wso2.balana.openaz.provider.AzServiceFactory;
import org.wso2.balana.openaz.provider.SimpleConcreteBalanaService;

public class HelloWorld {

	public static void main(String[] args) throws ParsingException,
			UnknownIdentifierException {

		SimpleConcreteBalanaService service = new SimpleConcreteBalanaService(
				null, "resources");
		AzServiceFactory.registerDefaultProvider(service);

		AzService azHandle = AzServiceFactory.getAzService();

		AzRequestContext azReqCtx = azHandle.createAzRequestContext();

		AzEntity<AzCategoryIdSubjectAccess> accSubj = azReqCtx
				.createAzEntity(AzCategoryIdSubjectAccess.AZ_CATEGORY_ID_SUBJECT_ACCESS);

		accSubj.createAzAttribute("AuthNSystem",
				AzXacmlStrings.X_ATTR_SUBJECT_ID,

				accSubj.createAzAttributeValue(
						AzDataTypeIdString.AZ_DATATYPE_ID_STRING, "alice"));

		azReqCtx.addAzEntity(accSubj);

		AzEntity<AzCategoryIdResource> azResource = azReqCtx
				.createAzEntity(AzCategoryIdResource.AZ_CATEGORY_ID_RESOURCE);

		azResource.createAzAttribute("applicationName",
				AzXacmlStrings.X_ATTR_RESOURCE_ID, azResource
						.createAzAttributeValue(
								AzDataTypeIdString.AZ_DATATYPE_ID_STRING,
								"bbb"));

		azReqCtx.addAzEntity(azResource);

		AzEntity<AzCategoryIdAction> azAction = azReqCtx
				.createAzEntity(AzCategoryIdAction.AZ_CATEGORY_ID_ACTION);

		azAction.createAzAttribute("applicationName",
				AzXacmlStrings.X_ATTR_ACTION_ID, azAction
						.createAzAttributeValue(
								AzDataTypeIdString.AZ_DATATYPE_ID_STRING,
								"read"));

		azReqCtx.addAzEntity(azAction);
		
		Set <AzEntity<AzCategoryIdAction>> azActions = new HashSet<AzEntity<AzCategoryIdAction>>();
		azActions.add(azAction);
		azReqCtx.addResourceActionAssociation(azResource, azActions);

		AzResponseContext azRspCtx = azHandle.decide(azReqCtx);

		AzResult azResult = null;

		Iterator<AzResult> itResults = azRspCtx.getResults().iterator();
		

		if (itResults.hasNext()) {
			azResult = itResults.next();
			System.out.println(azResult.getAzDecision());
		}

	}
}
