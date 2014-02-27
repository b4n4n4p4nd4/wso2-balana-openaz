package org.wso2.balana.openaz.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;

import javax.security.auth.x500.X500Principal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openliberty.openaz.azapi.AzAttributeValueBoolean;
import org.openliberty.openaz.azapi.AzEntity;
import org.openliberty.openaz.azapi.AzRequestContext;
import org.openliberty.openaz.azapi.AzResourceActionAssociation;
import org.openliberty.openaz.azapi.AzResourceActionAssociationId;
import org.openliberty.openaz.azapi.AzResponseContext;
import org.openliberty.openaz.azapi.AzAttribute;
import org.openliberty.openaz.azapi.AzAttributeValue;
import org.openliberty.openaz.azapi.AzDataDateTime;
import org.openliberty.openaz.azapi.AzAttributeValueString;
import org.openliberty.openaz.azapi.AzAttributeValueAnyURI;
import org.openliberty.openaz.azapi.AzObligations;
import org.openliberty.openaz.azapi.AzResult;
import org.openliberty.openaz.azapi.AzAttributeFinder;
import org.openliberty.openaz.azapi.constants.AzCategoryId;
import org.openliberty.openaz.azapi.constants.AzCategoryIdAction;
import org.openliberty.openaz.azapi.constants.AzCategoryIdEnvironment;
import org.openliberty.openaz.azapi.constants.AzCategoryIdObligation;
import org.openliberty.openaz.azapi.constants.AzCategoryIdResource;
import org.openliberty.openaz.azapi.constants.AzCategoryIdStatusDetail;
import org.openliberty.openaz.azapi.constants.AzCategoryIdSubjectAccess;
import org.openliberty.openaz.azapi.constants.AzCategoryIdSubjectCodebase;
import org.openliberty.openaz.azapi.constants.AzDataTypeIdAnyURI;
import org.openliberty.openaz.azapi.constants.AzDataTypeIdBoolean;
import org.openliberty.openaz.azapi.constants.AzDataTypeIdDateTime;
import org.openliberty.openaz.azapi.constants.AzDataTypeIdInteger;
import org.openliberty.openaz.azapi.constants.AzDataTypeIdString;
import org.openliberty.openaz.azapi.constants.AzDataTypeIdX500Name;
import org.openliberty.openaz.azapi.constants.AzDecision;
import org.openliberty.openaz.azapi.constants.AzStatusCode;
import org.openliberty.openaz.azapi.constants.AzXacmlStrings;
import org.wso2.balana.openaz.AbstractService;
import org.wso2.balana.openaz.AzAttributeValueBooleanImpl;
import org.wso2.balana.openaz.AzEntityImpl;
import org.wso2.balana.openaz.AzStatusMessagesImpl;
import org.wso2.balana.openaz.AzAttributeValueStringImpl;
import org.wso2.balana.openaz.AzAttributeValueAnyURIImpl;
import org.openliberty.openaz.azapi.constants.AzDataTypeId;
import org.openliberty.openaz.azapi.constants.AzDataTypeIdString;

// balana imports:
import org.wso2.balana.ConfigurationStore;
import org.wso2.balana.PDP;
import org.wso2.balana.PDPConfig;
import org.wso2.balana.ParsingException;
import org.wso2.balana.UnknownIdentifierException;
import org.wso2.balana.cond.FunctionFactory;
import org.wso2.balana.cond.FunctionFactoryProxy;
import org.wso2.balana.cond.StandardFunctionFactory;
import org.wso2.balana.finder.AttributeFinder;
import org.wso2.balana.finder.AttributeFinderModule;
import org.wso2.balana.finder.PolicyFinder;
import org.wso2.balana.finder.impl.CurrentEnvModule;
//import org.wso2.balana.finder.impl.FilePolicyModule;
import org.wso2.balana.finder.impl.FileBasedPolicyFinderModule;
import org.wso2.balana.finder.impl.SelectorModule;

// balana classes needed to do the attribute mapping:
import org.wso2.balana.Indenter;
import org.wso2.balana.ctx.xacml2.RequestCtx;
import org.wso2.balana.ctx.Attribute;
import org.wso2.balana.ctx.xacml2.Subject;
import org.wso2.balana.attr.AttributeValue;
import org.wso2.balana.attr.BooleanAttribute;
import org.wso2.balana.attr.IntegerAttribute;
import org.wso2.balana.attr.StringAttribute;
import org.wso2.balana.attr.DateTimeAttribute;
import org.wso2.balana.attr.AnyURIAttribute;
import org.wso2.balana.attr.X500NameAttribute;
import org.wso2.balana.ctx.ResponseCtx;
import org.wso2.balana.ctx.xacml2.Result;
import org.wso2.balana.ctx.Status;
import org.wso2.balana.ctx.StatusDetail;
import org.wso2.balana.xacml2.Obligation;

// import of balana sample to leave SimplePDP code intact
import org.wso2.balana.openaz.provider.balana.TimeInRangeFunction;
import org.wso2.balana.openaz.resources.OpenAzResourceDirectory;
import org.wso2.balana.openaz.resources.OpenAzResourceQueryBuilder;
import org.wso2.balana.openaz.resources.OpenAzBalanaAttributeFinderModule;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

//line below is expected max printable width                                   3210
/**********************************************************************************
 * balana service provider implementation.
 * This class is called by AzApi, converts the AzRequestContext
 * to the balana RequestCtx, calls the balana service 
 * and takes the returned balana ResponseCtx and 
 * converts it to an AzApi AzResponseContext and returns.
 * <p>
 * This module uses the balana SimplePDP.java sample as a
 * guide.
 * 
 * 
 * @author rlevinson
 *
 */
public class SimpleConcreteBalanaService extends AbstractService {
	
	// balana PDP
	private PDP pdp = null;
	private PDPConfig pdpConfig = null;
	private Map<String,AzRequestContext> azReqCtxMap = null;
	private List<AzAttributeFinder> azAttrFinderList = null;
    private String requestFile = null;
    
    /**
     * These URIs are for use getting context from AzService
     * environment in order to invoke AzApi callback
     */
    // subject codebase metadata for added attributes
    public static final String subjectCodebaseCategory =
    	AzCategoryIdSubjectCodebase.
    		AZ_CATEGORY_ID_SUBJECT_CODEBASE.toString();
    public static final String subjectCodebaseDataType =
    	AzDataTypeIdString.AZ_DATATYPE_ID_STRING.toString();
    public static final String subjectCodebaseIssuer =
    	"openaz:balana:azserviceimpl";
    
    private static int azRequestCounter = 0;
    private static int azServiceCounter = 0;
	static Log logStatic = LogFactory.getLog(SimpleConcreteBalanaService.class);
 	static StringWriter swStatic = new StringWriter();
    
    // attribute ids for attrs added to subject codebase,
    // that identify an AzEntity, where the value is the
    // value returned by getId() on AzEntity or AzRequestContext
    public static final String requestAzRequestContextAttributeId =
    	"urn:openaz:azrequestcontext:request:id";
    public static final String subjectAzEntityAttributeId =
    	"urn:openaz:azentity:subject:id";
    public static final String environmentAzEntityAttributeId =
    	"urn:openaz:azentity:environment:id";
    public static final String resourceAzEntityAttributeId =
    	"urn:openaz:azentity:resource:id";
    public static final String actionAzEntityAttributeId =
    	"urn:openaz:azentity:action:id";
    // URIs to hold the attribute data
    private static URI subjectCodebaseURI = null;
    private static URI subjectCodebaseCategoryURI = null;
    private static URI subjectCodebaseDataTypeURI = null;
    private static URI subjectCodebaseIssuerURI = null;
    private static URI requestAzRequestContextAttributeIdURI = null;
    private static URI subjectAzEntityAttributeIdURI = null; 
    private static URI resourceAzEntityAttributeIdURI = null; 
    private static URI actionAzEntityAttributeIdURI = null; 
    private static URI environmentAzEntityAttributeIdURI = null; 
    // initialize the standard subject identifier
    static {
        try {
        	// meta data for the subject codebase attributes:
 			subjectCodebaseURI =
 				new URI(AzCategoryIdSubjectCodebase.
 						AZ_CATEGORY_ID_SUBJECT_CODEBASE.toString());
        	subjectCodebaseCategoryURI = 
        		new URI(subjectCodebaseCategory);
            subjectCodebaseDataTypeURI = 
            	new URI(subjectCodebaseDataType); 
        	subjectCodebaseIssuerURI =
        		new URI(subjectCodebaseIssuer);
        	// attributeIds for the individual objects that can 
        	// be referenced; in general should be one for each
        	// possible category. In case of overlap w a real
        	// subject codebase, the attributeIds should keep
        	// things separated, as these are only added within
        	// the context of this request
        	requestAzRequestContextAttributeIdURI = 
            	new URI(requestAzRequestContextAttributeId); 
            subjectAzEntityAttributeIdURI = 
            	new URI(subjectAzEntityAttributeId); 
            resourceAzEntityAttributeIdURI = 
            	new URI(resourceAzEntityAttributeId); 
            actionAzEntityAttributeIdURI = 
            	new URI(actionAzEntityAttributeId); 
            environmentAzEntityAttributeIdURI = 
            	new URI(environmentAzEntityAttributeId); 
        } catch (URISyntaxException urise) {
            // won't happen in this code
			urise.printStackTrace(new PrintWriter(swStatic));
 			logStatic.error("unexpected error creating known URI:" +
 					urise.getMessage() + swStatic);
        }
    };
	public final static String PDP_CONFIG_PROPERTY =
        "org.wso2.balana.PDPConfigFile";
	Log log = LogFactory.getLog(this.getClass());
	
	/**
	 * Default balana constructor, which uses 
	 * {@link ConfigurationStore#getDefaultPDPConfig()} to set up
	 * the factories, etc.
	 * @throws ParsingException
	 * @throws UnknownIdentifierException
	 */
	public SimpleConcreteBalanaService() 
			throws ParsingException, 
				   UnknownIdentifierException {
		// Initialize the balana Service:
		String configFile = System.getProperty(PDP_CONFIG_PROPERTY);
		// if (log.isTraceEnabled()) log.trace(
		log.info( "Default constructor: will use policy files " +
			"listed in config file that is specified by " +
			"\n\tSystem Property: " + PDP_CONFIG_PROPERTY +
			",\n\twhich currently has value: " + configFile);
		ConfigurationStore store = new ConfigurationStore();
		store.useDefaultFactories();
		pdpConfig = store.getDefaultPDPConfig();
		pdp = new PDP(pdpConfig);
		init();
	}
	
	/**
	 * balanaService constructor with a requestFile
	 * as input and a set of policy files as input
	 * to configure for the PDP
	 * @param requestFile a file containing xacml request
	 * @param policyStore path of the directory that contains policy.xml files
	 * @throws ParsingException
	 * @throws UnknownIdentifierException
	 */
	public SimpleConcreteBalanaService(
				String requestFile,
				String policyStore) 
			throws ParsingException, 
				   UnknownIdentifierException {
		
		log.info( "Parameterized constructor: will use policy files " +
				"provided by parameter: " + 
				"\n\tpolicyFiles[] = " + policyStore);
		
		// Initialize the balana Service:

		this.requestFile = requestFile;
        System.setProperty(FileBasedPolicyFinderModule.POLICY_DIR_PROPERTY,policyStore);
        FileBasedPolicyFinderModule filePolicyModule = new FileBasedPolicyFinderModule();
        filePolicyModule.loadPolicies();
        
//        for (int i = 0; i < policyFiles.length; i++)
//        {
//        	File file = new File(policyFiles[i]);
//        	System.setProperty(FileBasedPolicyFinderModule.POLICY_DIR_PROPERTY,file.getParent());
//        	filePolicyModule.loadPolicies();
//        	      	
////            filePolicyModule.addPolicy(policyFiles[i]);
//            if (log.isTraceEnabled()) log.trace(
//            	"Added policy file: " + policyFiles[i]);
//        }

        
        // next, setup the PolicyFinder that this PDP will use
        PolicyFinder policyFinder = new PolicyFinder();
        Set policyModules = new HashSet();
        policyModules.add(filePolicyModule);
        policyFinder.setModules(policyModules);

        // now setup attribute finder modules for the current date/time and
        // AttributeSelectors (selectors are optional, but this project does
        // support a basic implementation)
        CurrentEnvModule envAttributeModule = new CurrentEnvModule();
        SelectorModule selectorAttributeModule = new SelectorModule();
        OpenAzBalanaAttributeFinderModule openAzFinderModule =
        	new OpenAzBalanaAttributeFinderModule();

        // Setup the AttributeFinder just like we setup the PolicyFinder. Note
        // that unlike with the policy finder, the order matters here. See the
        // the javadocs for more details.
        AttributeFinder attributeFinder = new AttributeFinder();
        List attributeModules = new ArrayList();
        attributeModules.add(envAttributeModule);
        attributeModules.add(selectorAttributeModule);
        attributeModules.add(openAzFinderModule);
        attributeFinder.setModules(attributeModules);

        // Try to load the time-in-range function, which is used by several
        // of the examples...see the documentation for this function to
        // understand why it's provided here instead of in the standard
        // code base.
        FunctionFactoryProxy proxy =
            StandardFunctionFactory.getNewFactoryProxy();
        FunctionFactory factory = proxy.getConditionFactory();
        factory.addFunction(new TimeInRangeFunction());
        FunctionFactory.setDefaultFactory(proxy);

        // finally, initialize our pdp
        pdpConfig = new PDPConfig(attributeFinder, policyFinder, null);
        pdp = new PDP(pdpConfig);
        
        // init structures common to all constructors
        init();
	}
	
	/**
	 * misc init tasks, such as setting the appropriate context for
	 * {@link OpenAzBalanaAttributeFinderModule#setAzRequestContextMap(Map)},
	 * which enables the single request balana pdp to gain access to the
	 * full {@link AzRequestContext}, which has broader information.
	 * 
	 */
	void init() {
		// sanity check on log state only use if debug or trace mode
		if (log.isDebugEnabled()) {
			log.info("\n    log.getLogger() class = " + log.getClass().getName());
	        log.info("\n    Log = " + log.getClass() +
	        		"\n\tisFatalEnabled() = " + log.isFatalEnabled() +
	        		"\n\tisErrorEnabled() = " + log.isErrorEnabled() +
	        		"\n\tisWarnEnabled() = " + log.isWarnEnabled() +
	        		"\n\tisInfoEnabled() = " + log.isInfoEnabled() +
	        		"\n\tisDebugEnabled() = " + log.isDebugEnabled() +
	        		"\n\tisTraceEnabled() = " + log.isTraceEnabled() +
	        		"\n\tlog.getClass().getName() = " + log.getClass().getName());
	        //log.debug("Logging debug (Fine) instance"); 
	        // try logging at each level
	        log.fatal("Test log levels: start marker");
	        log.fatal("Logging Fatal   (Fatal)");
	        log.error("Logging Error   (Error)");
	        log.warn( "Logging Warning (Warn)");
	        log.info( "Logging Info    (Info)");
	        log.debug("Logging Debug   (Fine)");
	        log.trace("Logging Trace   (Finest)");
	        log.fatal("Test log levels: end marker\n");
		} // end if (isDebugEnabled())
		
		azServiceCounter++;
		log.info("AzService identifier, azServiceCounter = " + 
				azServiceCounter);
 		azReqCtxMap = new HashMap<String, AzRequestContext>();
		if ( ! (pdpConfig == null) ) {
			AttributeFinder attrFinder = pdpConfig.getAttributeFinder();
			List attrFinderModules = attrFinder.getModules();
			Iterator it = attrFinderModules.iterator();
			while (it.hasNext()) {
				AttributeFinderModule attrFinderModule = 
					(AttributeFinderModule) it.next();
				if (log.isTraceEnabled()) log.trace(
					"\n    Listing AttributeFinderModules: \n\tthis module = " +
						attrFinderModule.getClass().getName() + "\n");
				if (attrFinderModule instanceof 
						OpenAzBalanaAttributeFinderModule) {
					if (log.isTraceEnabled()) log.trace(
						"\n\tInitializing balanaAttributeFinderModule " +
							"with addr of AzRequestContext list.");
					OpenAzBalanaAttributeFinderModule openAzAttrFinderModule =
						(OpenAzBalanaAttributeFinderModule) attrFinderModule;
					// set the finder w a ref to the map of AzRequestContexts
					openAzAttrFinderModule.setAzRequestContextMap(azReqCtxMap);
					// init list of AzAttributeFinderModules:
					azAttrFinderList = new ArrayList<AzAttributeFinder>();
					azAttrFinderList.add(new SimpleDummyAzAttributeFinder());
					// set the finder w a ref to the AzAttributeFinder List
					// of registered azapi AzAttributeFinder modules:
					openAzAttrFinderModule.setAzAttributeFinderList(
							azAttrFinderList);
				}
			}
		}
		//if (log.isTraceEnabled()) log.trace(
		log.info("AttributeFinder list: azAttrFinderList = " + 
					azAttrFinderList);
	}
	
	/**
	 * Register a finder w balana:
	 * TODO: implement
	 */
	public <T extends Enum<T> & AzCategoryId>
		void registerAzAttributeFinder(
			AzAttributeFinder<T> azAttributeFinder) {
		log.info(
			"Not implemented yet, initial finders registered by constructors");
		
	}
	
	/**
	 * This module takes an {@link AzEntity} and converts the
	 * {@link AzAttribute}s to balana {@link Attribute}s that
	 * are returned in a Set which can be directly used by
	 * @param <T>
	 * @param azEntity
	 * @return a Set if balana Attributes
	 */
	public <T extends Enum<T> & AzCategoryId>
			Set getbalanaAttributeSet(AzEntity<T> azEntity){
      	StringWriter sw = new StringWriter();
      	if (log.isTraceEnabled() ) log.trace(
      		"\n    Convert AzAttributes to balana Attributes " +
      		"\n     from AzEntity with id: " + azEntity.getId());
		Set balanaAttrSet = new HashSet();
        Set<AzAttribute<T>> azAttrs = azEntity.getAzAttributeSet();
        Iterator<AzAttribute<T>> itAttrs = azAttrs.iterator();
        while (itAttrs.hasNext()) {
        	AzAttribute<T> azAttr = itAttrs.next();
        	if (log.isTraceEnabled()) log.trace(
        		"\n    AzAttribute metadata and value: " +
        		"\n\t AzCategoryId: " + azAttr.getAzCategoryId() +
        		"\n\t AttributeId: " + azAttr.getAttributeId() +
        		"\n\t DataType: " + azAttr.getAzAttributeValue().getType() +
        		"\n\t Value: " + azAttr.getAzAttributeValue().getValue());
        	AzDataTypeId azType = azAttr.getAzAttributeValue().getType();
        	
        	// initialize the balana value to null
        	AttributeValue balanaAttrValue = null;
        	if (azType.equals(AzDataTypeIdString.AZ_DATATYPE_ID_STRING)) {
        		if (log.isTraceEnabled()) log.trace(
        				"\n\tAzAttribute Type matches: " + azType +
        				"\n\t convert to balana: StringAttribute.");
        		//@SuppressWarnings("unchecked")
        		AzAttributeValue<?,?> azAttrValue = azAttr.getAzAttributeValue();
        		// Note: for ref wrt generics usage
        		// Above works, but, the following does not work:
        		// AzAttributeValue<AzDataTypeIdString,String> 
        		//   azAttrValue =
        		//     (AzAttributeValue<AzDataTypeIdString,String>)
        		//       azSubjAttr.getAzAttributeValue();
        		
        		balanaAttrValue = new StringAttribute(
        				azAttrValue.getValue().toString());
    		} else if (azType.equals(AzDataTypeIdBoolean.AZ_DATATYPE_ID_BOOLEAN)) {
        		if (log.isTraceEnabled()) log.trace(
        				"\n\tAzAttribute Type matches: " + azType +
        				"\n\t convert to balana: BooleanAttribute.");
        		//@SuppressWarnings("unchecked")
        		AzAttributeValue<?,?> azAttrValue = azAttr.getAzAttributeValue();
        		balanaAttrValue = BooleanAttribute.getInstance(
        				((Boolean)(azAttrValue.getValue())).booleanValue());
    		} else if (azType.equals(AzDataTypeIdInteger.AZ_DATATYPE_ID_INTEGER)) {
        		if (log.isTraceEnabled()) log.trace(
        				"\n\tAzAttribute Type matches: " + azType +
        				"\n\t convert to balana: IntegerAttribute(long)");
        		//@SuppressWarnings("unchecked")
        		AzAttributeValue<?,?> azAttrValue = azAttr.getAzAttributeValue();
        		balanaAttrValue = 
        			new IntegerAttribute((Long)azAttrValue.getValue());
        	} else if (azType.equals(
    			AzDataTypeIdDateTime.AZ_DATATYPE_ID_DATETIME)) {
        		if (log.isTraceEnabled()) log.trace(
        				"\n\tAzAttribute Type matches: " + azType +
        				"\n\t convert to balana: DateTimeAttribute.");
        		//@SuppressWarnings("unchecked")
        		AzAttributeValue<?,?> azAttrValue = azAttr.getAzAttributeValue();
        		AzDataDateTime azDataDateTime = 
        				(AzDataDateTime)azAttrValue.getValue();
        		// create the balana DateTimeAttribute
        		balanaAttrValue = new DateTimeAttribute(
        				azDataDateTime.getDate(), 
        				azDataDateTime.getNanoSecondOffset(),
        				azDataDateTime.getIntendedTimeZone(),
        				azDataDateTime.getActualTimeZone());       			
        	} else if (azType.equals(
        			AzDataTypeIdX500Name.AZ_DATATYPE_ID_X500NAME)) {
        		if (log.isTraceEnabled()) log.trace(
        				"\n\tAzAttribute Type matches: " + azType +
        				"\n\t convert to balana: X500NameAttribute.");
        		//@SuppressWarnings("unchecked")
        		AzAttributeValue<?,?> azAttrValue = azAttr.getAzAttributeValue();
        		balanaAttrValue = new X500NameAttribute(
        				(X500Principal)azAttrValue.getValue());
        	} else {
        		if (log.isTraceEnabled()) log.trace(
        				"No type match found for type: " + azType);
        	}
        	
        	if ( ! ( balanaAttrValue == null ) ) {
	        	// create a balana Attribute and add to set
	        	try {
	        		Attribute attr = new Attribute(
	        			new URI(azAttr.getAttributeId()),
	        			azAttr.getAttributeIssuer(),
	        			null,	// unused field for 2.0
	        			balanaAttrValue,2);
	        		balanaAttrSet.add(attr);
	        	} catch (URISyntaxException use) {
	    			use.printStackTrace(new PrintWriter(sw));
	    			if (log.isInfoEnabled()) log.info(
	    				"balana ParsingException: " +
	    					use.getMessage() + "\n" + sw);    		
	        	}
        	} else {
            	if (log.isWarnEnabled()) {
            	  log.warn("No balana Attribute added for:" +
            	    "\n   from AzEntity with id: " + azEntity.getId() +
    	        	"\n   while processing request number: " + azRequestCounter +
                	"\n\t AttributeId: " + azAttr.getAttributeId() +
                	"\n\t DataType: " + azAttr.getAzAttributeValue().getType() +
                	"\n\t Value: " + azAttr.getAzAttributeValue().getValue());
            	}
        	}
        } // end while more attrs
        if (log.isTraceEnabled()) log.trace(
        		"\n   Returning balana Attribute Set(" + 
        			azEntity.getAzCategoryId().getClass().getSimpleName() + 
        		"): length = " + balanaAttrSet.size() + "\n");
		return balanaAttrSet;
	}
	
    public AzResponseContext decide(AzRequestContext azReqCtx)
    {
  		StringWriter sw = new StringWriter();
        if (log.isTraceEnabled()){log.trace(
			"\n$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$" + 
			"$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$" +
    		"\n  Begin processing AzRequestContext " +
    		"\n\twith id = " + azReqCtx.getId() +
			"\n    Executing: \n\t" + 
			this.getClass().getName() + ".decide(azReqCtx): " +
			"\n$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$" + 
			"$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$\n");
        }
      	
      	// save the AzRequestContext, indexed by its instance id
      	// this enables attribute finders to find the context later,
      	// as the id is also passed to the balana pdp as a
      	// subject-codebase attribute, which is available in the 
      	// attribute finder module
      	azReqCtxMap.put(azReqCtx.getId(), azReqCtx);


        // Collect attributes for balana: 
      	//  - first collect the attributes once for the subject 
      	//     and environment, 
      	//  - then if multiple resource actions are requested, 
      	//     reuse the subject and env for each
        // (TBD: only one subject category implemented now)
      	// (however, subject-codebase used below for correlation)
        
        // Collect attributes for balana Subject
        AzEntity<AzCategoryIdSubjectAccess> azSubj =
        	azReqCtx.getAzEntity(
        		AzCategoryIdSubjectAccess.AZ_CATEGORY_ID_SUBJECT_ACCESS);
        Set newSubjects = new HashSet();
        Set newSubject = getbalanaAttributeSet(azSubj);
        
        // create a set for the metadata attributes using codebase subject
 		/*
 		URI subjectCodebaseURI = null;
 		try {
 			subjectCodebaseURI =
 				new URI(AzCategoryIdSubjectCodebase.
 						AZ_CATEGORY_ID_SUBJECT_CODEBASE.toString());
 		} catch (URISyntaxException up) {
 			// nothing to do here - it is a fixed known URI
			up.printStackTrace(new PrintWriter(sw));
 			log.error("unexpected error creating known URI:" +
 					up.getMessage() + sw);
 		}
 		*/

        // Since category is subject access use default balana
        // Subject constructor.
        Subject subj = new Subject(newSubject);
        newSubjects.add(subj);
        
        // Collect attributes for balana Environment 
        Set newEnvironment = new HashSet();
        AzEntity<AzCategoryIdEnvironment> azEnv = 
        	azReqCtx.getAzEntity(
            	AzCategoryIdEnvironment.AZ_CATEGORY_ID_ENVIRONMENT);
        if (azEnv != null)
        	newEnvironment = getbalanaAttributeSet(azEnv);
        
        // Create an AzResponseContext to store the results returned
        // from the balana calls
        AzResponseContextImpl azRspCtx = new AzResponseContextImpl();
        
        // Create the first AzResult which must be prepared for
        // return in case there are no AzResourceActionAssociations
        // to process.
        // TODO: need to add logic for the case where no r/a
        //  pairs have been submitted
        AzResultImpl azResult = new AzResultImpl();  
        
        // Set up a loop to create resource and action sets to pass
        // to the balana RequestCtx for each resource action assn:
        Set newResource = null;
        Set newAction = null;
        int balanaDecision = 0; // init a decision variable
        
        // Set up iterator and receiving variable to go thru
        // the AzResourceActionAssociations submitted w the
        // AzRequestContext passed to the decide method
        AzResourceActionAssociation azRaa = null;
        Set<AzResourceActionAssociation> azAssociations =
        	azReqCtx.getAssociations();
        Iterator<AzResourceActionAssociation> itRaa = 
        	azAssociations.iterator();
        if (log.isDebugEnabled()) log.debug(
    		"\n=============================================" + 
	  		"==============================================" +
        	"\n    Begin loop to submit one balana RequestCtx for each " +
        	"\n     AzResourceActionAssociation in the AzRequestContext." +
        	"\n\tNumber of AzResourceActionAssociations submitted = " +
        	azAssociations.size() +
			"\n=============================================" + 
		  	"==============================================\n");

        // Use the subject and env collected above repeatedly
        // in each request for an AzResourceActionAssociation.
        // Start of the loop for processing each AzResourceActionAssociation:
        while (itRaa.hasNext()){
        	// Get next AzResourceActionAssociation to process
        	azRaa = itRaa.next();       	
        	AzEntity<AzCategoryIdResource> azResource =
        		azRaa.getAzResource();
        	AzEntity<AzCategoryIdAction> azAction =
        		azRaa.getAzAction();
        	
        	// start a new request
        	azRequestCounter++;
        	if (log.isInfoEnabled()) {
        		StringBuffer sbAttrs = new StringBuffer();
	        	sbAttrs = getAllAttrList(azSubj,azResource,azAction,azEnv);
        		String subjectId = getSubjectId(azSubj);
        		if (subjectId == null)
        			subjectId = " (no attr w attrId = xacml:subject-id found)";
        		log.info(
	            	"\n#############################################" + 
	        	  	"##############################################" +
	        		"\n   Start processing request number: " + azRequestCounter +
	        		"\n     for AzResourceActionAssociation w correlationId = " +
	        			azRaa.getCorrelationId() +
	        		"\n\t\tSubject  AzEntity.getId: " + azSubj.getId() +
	        		"\n\t\tResource AzEntity.getId: " + 
	        			azRaa.getAzResource().getId() +
	        		"\n\t\tAction   AzEntity.getId: " + 
	        			azRaa.getAzAction().getId() +
	        		"\n\t\t requestor subject-id:  " + subjectId + 
	        		"\n\t\t requested resource-id: " +
	        			azRaa.getAzResourceActionAssociationId().getResourceId() +
	        		"\n\t\t requested action-id:   " +
	        			azRaa.getAzResourceActionAssociationId().getActionId() +
	        		"\n\t  All attributes: " + sbAttrs.toString() +
	        		"\n\t  Using AzService w azServiceCounter = " + 
	        			azServiceCounter +
	            	"\n#############################################" + 
	        	  	"##############################################\n");
        	}
       	
        	// re-initialize the subjects collection
        	newSubjects = new HashSet();
        	newSubjects.add(subj);
        	
        	// create balana attribute sets for resource and action
        	newResource = getbalanaAttributeSet(azResource);
        	newAction = getbalanaAttributeSet(azAction);
        	
        	// create a subject codebase identifying the AzRequestContext
        	// and the AzEntity objects used in this request, which enable
        	// a balana callback to reconstruct context to initiate
        	// an azapi callback at the higher level
     		Set balanaMetaAttrSet = new HashSet();
    		balanaMetaAttrSet.add( new Attribute(
    			requestAzRequestContextAttributeIdURI,
    			subjectCodebaseIssuer, null,
    			new StringAttribute(azReqCtx.getId()),2));
    		balanaMetaAttrSet.add( new Attribute(
    				subjectAzEntityAttributeIdURI,
        			subjectCodebaseIssuer, null,
        			new StringAttribute(azSubj.getId()),2));
    		balanaMetaAttrSet.add( new Attribute(
    				resourceAzEntityAttributeIdURI,
        			subjectCodebaseIssuer, null,
        			new StringAttribute(azResource.getId()),2));
    		balanaMetaAttrSet.add( new Attribute(
    				actionAzEntityAttributeIdURI,
        			subjectCodebaseIssuer, null,
        			new StringAttribute(azAction.getId()),2));
    		if ( ! (azEnv == null) ) {
    			balanaMetaAttrSet.add( new Attribute(
        				environmentAzEntityAttributeIdURI,
            			subjectCodebaseIssuer, null,
            			new StringAttribute(azEnv.getId()),2));
    		}
            Subject metaSubject = 
            	new Subject(subjectCodebaseURI, balanaMetaAttrSet);
    		newSubjects.add(metaSubject);
        
	        // Construct the balana RequestCtx from the subject and 
        	// environment attribute sets collected above plus the 
        	// resource and action for the current iteration of the loop
	        RequestCtx reqCtx = new RequestCtx(
	        	newSubjects, newResource, newAction, newEnvironment);
	        
	        // For the purpose of demonstrating the ability of the 
	        // balana package to produce XACML XML Request and
	        // Response documents (messages) the encode method will
	        // be called below. However, the xml itself is not used
	        // for any further processing.
	        ByteArrayOutputStream bo = null;
	        if (log.isTraceEnabled()) { 
	        	log.trace(
	        		"\n\tbalana Request XML from RequestCtx follows:" +
	        		"\n\t (Note: subject-codebase is created for use)" +
	        		"\n\t (w attr finders for internal correlation)\n");
		        bo = new ByteArrayOutputStream();
		    	reqCtx.encode(bo, new Indenter());
	        	log.trace("\n" + bo.toString());
	        }
	    	System.out.println("fgljdfg");
	        ResponseCtx rspCtx = pdp.evaluate(reqCtx);
	        
	        
	        // Produce the XML for the Response (demo only, not needed)
	        if (log.isTraceEnabled()) {
	        	log.trace(
	        		"\n\tbalana Response XML from ResponseCtx follows:\n");
		        bo = new ByteArrayOutputStream();
		        //rspCtx.encode(System.out, new Indenter());
		        rspCtx.encode(bo, new Indenter());
		        log.trace("\n" + bo.toString());
	        }

	        // now need to create a result to add to the
	        // the azRspCtx
	        int balanaResultCounter = 0; // use to distinguish multi-results
	        Set balanaResultSet = rspCtx.getResults();
	        Iterator itbalanaResult = balanaResultSet.iterator();
	        while (itbalanaResult.hasNext()){
	        	balanaResultCounter++;
	        	Result balanaResult = (Result) itbalanaResult.next();
	        	azResult = new AzResultImpl();
	        	azResult.setAzResourceActionAssociation(azRaa);
	        	if (log.isTraceEnabled()) log.trace(
	        		"\n    Status.getCode() = " + 
	        		balanaResult.getStatus().getCode() + 
	        		"\n\tStatus.getCode().get(0) = " + 
	        		balanaResult.getStatus().getCode().get(0) +
	        		"\n\tStatus.STATUS_OK = " + Status.STATUS_OK);
        		String balanaStatusCode = 
        			(String) balanaResult.getStatus().getCode().get(0);
        		String balanaStatusMessage = 
        			balanaResult.getStatus().getMessage();
        		String statusText = "";
        		// Set the status code now if it is "OK" because the "non-OK"
        		// values only appear for the Indeterminate decision code
        		if (balanaStatusCode.equals(Status.STATUS_OK)){
        			azResult.setAzStatusCode(AzStatusCode.AZ_OK);
	    			statusText = "\n\t StatusCode = " + balanaStatusCode;
        			if (log.isTraceEnabled()) log.trace(
        				"Setting AzStatusCode to: " + AzStatusCode.AZ_OK);
        		} else {
	    			statusText = "\n\t StatusCode = " + balanaStatusCode +
						"\n\t  StatusMessage = \"" + balanaStatusMessage + "\"";        			
        		}
        		
        		// Process each possible returned balana decision/status combo
        		//  separately, and set the corresponding AzResult data element 
	        	balanaDecision = balanaResult.getDecision();
        		String decisionText =  
        			" (" + balanaDecision + ")" + statusText; 
	        	switch (balanaDecision) {
	        		// Normal processing conditions where policies
	        		// fully evaluated
		        	case Result.DECISION_PERMIT:
		        		azResult.setAzDecision(AzDecision.AZ_PERMIT);
		        		log.info("\n\tDecision = Permit" + decisionText + "\n");
		        		break;
		        	case Result.DECISION_DENY:
		        		azResult.setAzDecision(AzDecision.AZ_DENY);
		        		log.info("\n\tDecision = Deny" + decisionText + "\n");
		        		break;
		        	case Result.DECISION_NOT_APPLICABLE:
		        		azResult.setAzDecision(AzDecision.AZ_NOTAPPLICABLE);
		        		log.info("\n\tDecision = NotApplicable" + 
		        				decisionText + "\n");
		        		break;
		        	// "abnormal" processing conditions, where policies
		        	// were not fully evaluated
		        	case Result.DECISION_INDETERMINATE:
		        		azResult.setAzDecision(AzDecision.AZ_INDETERMINATE);
		        		if (balanaStatusCode.equals(
		        				Status.STATUS_MISSING_ATTRIBUTE)){
			        		// Policy not fully evaluated due to missing info
			        		// in the request (this is a "normal" occurrence
			        		// that is handled by reporting a missing attr
			        		// and possibly recoverable by resubmitting req
			        		// w missing attrs included
			        		azResult.setAzStatusCode(
			        			AzStatusCode.AZ_MISSING_ATTRIBUTE);
			        		log.info("\n\tDecision = Indeterminate: " + 
		        				decisionText + " (MissingAttribute - " + 
		        				"If it can be provided, then may try again.)\n");
		        		} else if (balanaStatusCode.equals(
		        				Status.STATUS_PROCESSING_ERROR)){
		        			// This case represents a "structural" policy
		        			// error, where policy evaluation failed, where
		        			// all info was probably ok, but the policies
		        			// themselves may not be set up properly to
		        			// handle specific situations (ex. divide by zero,
		        			// multiple applicable top level policies (only
		        			// one top level "applicable" policy is allowed.
	        				azResult.setAzStatusCode(
	        					AzStatusCode.AZ_PROCESSING_ERROR);
			        		log.error(
			        			"\n\tDecision = Indeterminate" + decisionText +
	        						"\n\t\t(XACML ProcessingError - " +
	        						"Check Policy Evaluation Logic)\n");
		        		} else if (balanaStatusCode.equals(
		        				Status.STATUS_SYNTAX_ERROR)){
		        			// This case generally represents bad attribute
		        			// values of some sort that do not syntactically
		        			// agree with the Xacml DataType.
	        				azResult.setAzStatusCode(
	        					AzStatusCode.AZ_SYNTAX_ERROR);
			        		log.error(
			        			"\n\tDecision = Indeterminate" + decisionText +
        							"\n\t\t(XacmlSyntaxError - " +
        							"Check AttributeValues)\n");
		        		} else {
			        		log.error(
			        			"\n\tDecision = Indeterminate" + statusText +
								"\n\t\t (UnknownStatusCodeError - " +
			        				"Check for non-XACML types of errors)\n");
		        		}
		        		break;
	        	} // end switch on balanaDecision
	        	if (log.isTraceEnabled()) log.trace(
	        		"\n    Decision and Status codes: " +
        			"\n\tbalanaDecision: " + balanaDecision +
        			"\n\tfor example the enum: Result.DECISION_PERMIT = " + 
        			"will display as an integer: " + Result.DECISION_PERMIT +
        			"\n\t\tazDecision: " + azResult.getAzDecision() +
        			"\n\tbalanaStatusCode: " + balanaStatusCode +
        			"\n\t\tazStatusCode: " + azResult.getAzStatusCode() +
        			"  " + azResult.getAzStatusCode().toString());
	        	
	        	// Store the status message, if any, in the AzResult
	        	azResult.setStatusMessage(
	        		balanaResult.getStatus().getMessage());
	        	
	        	// If there are any Obligations, process them
	        	if ( !(balanaResult.getObligations().isEmpty()) ) {
	        		if (log.isTraceEnabled()) log.trace(
	        				"Found some obligations");
	        		
	        		// Set up an AzObligations object to hold the AzObligations
	        		AzObligations azObligations = new AzObligationsImpl();
	        		
	        		// set up loop to process each obligation
	        		Set balanaObligations = (Set) balanaResult.getObligations();
	        		Iterator itbalanaObligations = 
	        			balanaObligations.iterator();
	        		while (itbalanaObligations.hasNext()){
	        			if (log.isTraceEnabled()) log.trace(
	        					"Processing next obligation");
	        			Obligation balanaObligation = 
	        				(Obligation) itbalanaObligations.next();
	        			
	        			// Note: for obls 
	        			//		use createNewAzEntity(), 
	        			//		not createAzEntity(), 
	        			// to create an AzEntity<AzCategoryObligationId> because 
	        			// createAzEntity() adds the AzEntity to the"request" 
	        			// whereas createNewAzEntity creates a free AzEntity,
	        			// which can independently be added to the response. 
	        			// TODO: probably need to clean up azapi in this regard
	        			AzEntity<AzCategoryIdObligation> azObligation =
	        				azReqCtx.createNewAzEntity(
	        					AzCategoryIdObligation.AZ_CATEGORY_ID_OBLIGATION);
	        			
	        			// Set the AzEntity<AzCategoryIdObligation> with the data
	        			// from the returned balana Obligation
	        			// Set the ObligationId in the 
	        			azObligation.setAzEntityId(
	        					balanaObligation.getId().toString());
	        			
	        			//TODO: need a place in AzEntity<AzCategoryIdObligaiton>
	        			// for the xacml FulfillOn xml attribute
	        			
	        			// Set up loop to process each AttributeAssignment within
	        			// the returned balana Obligation
	        			List balanaAttrAssigns = 
	        				balanaObligation.getAssignments();
	        			if (log.isTraceEnabled()) log.trace(
	        				"Obligation balanaAttrAssigns = " + 
	        					balanaAttrAssigns);
	        			Iterator itbalanaAttrAssigns = 
	        				balanaAttrAssigns.iterator();
	        			// Start of Obligation attributes loop:
	        			while (itbalanaAttrAssigns.hasNext()) {
	        				if (log.isTraceEnabled()) log.trace(
	        						"Processing next AttrAssign");
	        				Attribute balanaAttrAssign = 
	        					(Attribute) itbalanaAttrAssigns.next();
	        				AttributeValue balanaAttrAssignValue = 
	        					balanaAttrAssign.getValue();
	        				URI balanaType = balanaAttrAssignValue.getType();
	        				//AzAttributeValue<?,?> azAttrValue = null;
	        				if (log.isTraceEnabled()) log.trace(
	        						"Type test: " +
	        						"\n\tbalanaType = " + balanaType +
	        						"\n\tStringAttribute.identifer = " +
	        						StringAttribute.identifier +
	        						"\n\tAnyURIAttribute.identifer = " +
	        						AnyURIAttribute.identifier);
	        				// Variable to hold the mapped AzAttribute data from
	        				//  the balana Attribute
	        				AzAttribute<?> azAttr = null;
	        				
	        				// Each subtype of balana Attribute has to be handled
	        				// distincly to create the correct corresponding
	        				// AzAttributeValue<AzDataTypeId*, JavaObject> as 
	        				// defined in azapi.constants; Note: the JavaObjects
	        				// used by azapi should be similar if not equal to 
	        				// those used by balana as balana was part of 
	        				// guidance used in development requirements of azapi
	        				
	        				// case: sun.xacml.StringAttribute:
	        				if (balanaType.toString().equals(
	        						StringAttribute.identifier)) {
	        				  if (log.isTraceEnabled()) log.trace(
	        					  "\n\tFound an attr type: " + 
	        						StringAttribute.identifier);
	        				  String value = ((StringAttribute)
	        					balanaAttrAssignValue).getValue();
	        				  AzAttributeValue<AzDataTypeIdString,String> 
	        					azAttrValue = (AzAttributeValueString) 
	        					  new AzAttributeValueStringImpl(
	        					    ((StringAttribute)balanaAttrAssignValue).
	        					    	getValue());
		        			  azAttr = 
		        				azObligation.createAzAttribute(
		        					balanaAttrAssign.getIssuer(),
		        					balanaAttrAssign.getId().toString(), 
		        					azObligation.createAzAttributeValue(
		        						AzDataTypeIdString.AZ_DATATYPE_ID_STRING, 
		        						value));
		        					//getAzAttributeValue(
	        						//	AzDataTypeIdString.AZ_DATATYPE_ID_STRING,
	        						//	value, azObligation));
		        			// end case sun.xacml.StringAttribute
		        			  
		        			// case sun.xacml.AnyURIAttribute:
	        				} else if (balanaType.toString().equals(
	        						AnyURIAttribute.identifier)) {
	        				  if (log.isTraceEnabled()) log.trace(
	        						"\n\tFound an attr type: " + 
	        							AnyURIAttribute.identifier);
	        				  URI value = ((AnyURIAttribute)
	        						balanaAttrAssignValue).getValue();
	        				  	if ( !(value == null) )
	        						if (log.isTraceEnabled()) log.trace(
	        							"\n\tFound URI value = " + 
	        								value.toString());
	        				  	else
	        						if (log.isTraceEnabled()) log.trace(
	        							"\n\tFound null URI value = ");
	        						
	        				  	AzAttributeValue<AzDataTypeIdAnyURI,URI> 
	        				  	  azAttrValue = (AzAttributeValueAnyURI) 
	        						new AzAttributeValueAnyURIImpl(
	        						  ((AnyURIAttribute)
	        							balanaAttrAssignValue).getValue());
	        					if ( !(azAttrValue == null))
	        					  if (log.isTraceEnabled()) log.trace(
	        						"\n\tCreated an AzAttributeValueAnyURI value = " + 
	        						azAttrValue.toString());
		        				azAttr = 
		        				  azObligation.createAzAttribute(
			        				balanaAttrAssign.getIssuer(),
			        				balanaAttrAssign.getId().toString(), 
			        				azObligation.createAzAttributeValue(
			        					AzDataTypeIdAnyURI.AZ_DATATYPE_ID_ANYURI, 
			        					value));
		        				
	        				} else if (balanaType.toString().equals(
		        						BooleanAttribute.identifier)) {
		        				  if (log.isTraceEnabled()) log.trace(
		        					  "\n\tFound an attr type: " + 
		        						BooleanAttribute.identifier);
		        				  Boolean value = ((BooleanAttribute)
		        					balanaAttrAssignValue).getValue();
		        				  AzAttributeValue<AzDataTypeIdBoolean,Boolean> 
		        					azAttrValue = (AzAttributeValueBoolean) 
		        					  new AzAttributeValueBooleanImpl(
		        					    ((BooleanAttribute)balanaAttrAssignValue).
		        					    	getValue());
			        			  azAttr = 
			        				azObligation.createAzAttribute(
			        				  balanaAttrAssign.getIssuer(),
			        				  balanaAttrAssign.getId().toString(), 
			        				  azObligation.createAzAttributeValue(
			        					AzDataTypeIdBoolean.AZ_DATATYPE_ID_BOOLEAN, 
			        					value));
		        			} // end case: sun.xacml.BooleanAttribute
			        			  
	        				if (log.isTraceEnabled()) log.trace(
	        						"\n  azAttr = " + azAttr);
	        				if (log.isTraceEnabled()) log.trace(
	        					"\n    Attribute:" +
        						"\n\tAttributeId = " + azAttr.getAttributeId() +
        						"\n\tAttributeIssuer = " + 
        						azAttr.getAttributeIssuer() +
        						//"\n\tDataType = " + 
        						//azAttr.getAzAttributeValue().getType() +
        						"\n\tAttributeValue = " + 
        						azAttr.getAzAttributeValue());	        					        					
	        			} // end while loop AttributeAssignments
	        			if (log.isTraceEnabled()) log.trace(
	        					"\n    End of AttrAssign loop\n");
	        			azObligations.addAzObligation(azObligation);
	        		} // end while loop obligations
	        		azResult.setAzObligations(azObligations);
	        	} // end if any obligations
	        	
	        	//TODO: need to add processing of StatusDetail,
	        	// in particular, adding missing attributes
	        	StatusDetail balanaStatusDetail = 
	        		balanaResult.getStatus().getDetail();
	        	if ( !(balanaStatusDetail == null) ) {
	        		Node balanaStatusDetailRoot =
	        			balanaStatusDetail.getDetail();
	        		if ( balanaStatusDetailRoot.hasChildNodes()) {
	        			NodeList attributeNodes = 
	        				balanaStatusDetailRoot.getChildNodes();
	        			Set balanaAttributes = new HashSet();
	        			int attrNodeListLength = attributeNodes.getLength();
	        			for (int i = 0; i < attrNodeListLength; i++) {
	        				//TODO: Verify node is an Attribute, and process
	        				Node node = attributeNodes.item(i);
	        				String name = node.getNodeName();
	        				try {
		        				if (name.equals("Attribute")) {
		        					// create a balana node
		        					Attribute balanaAttr = 
		        						Attribute.getInstance(node);
		        					balanaAttributes.add(balanaAttr);
		        					if (log.isTraceEnabled()) log.trace(
		        						"balana returned a missing attribute: " +
	        							balanaAttr.encode());
		        				} else {
		        					// Don't know what it could be so append
		        					// its node name to the status message
		        					String statusMessage = 
		        					  azResult.getStatusMessage() + 
	        						  " Found StatusDetail child with NodeName: " + 
	        						  name;
		        					azResult.setStatusMessage(statusMessage);
		        					if (log.isTraceEnabled()) log.trace(
		        							statusMessage);
		        				}
	        				} catch (ParsingException pe) {
	        					pe.printStackTrace(new PrintWriter(sw));
	        					if (log.isTraceEnabled()) log.trace(
	        						"Could not getInstance of node type: " + 
	        						name);
	        				}
	        			} // end missing attributes loop
	        			if ( ! balanaAttributes.isEmpty() ) {
	        				// Add the found attributes as missing attributes
	        				AzEntity<AzCategoryIdStatusDetail> azStatusDetail =
	        					azReqCtx.createNewAzEntity(
	        						AzCategoryIdStatusDetail.
	        							AZ_CATEGORY_ID_STATUSDETAIL);
	        				for (Iterator i = balanaAttributes.iterator(); 
	        						i.hasNext();){
	        					Attribute balanaAttr = (Attribute)i.next();
	        					azStatusDetail.createAzAttribute(
	        						balanaAttr.getIssuer(),
	        						balanaAttr.getId().toString(),
	        						azStatusDetail.createAzAttributeValue(
	    	        					AzDataTypeIdString.AZ_DATATYPE_ID_STRING,
	    	        					balanaAttr.getValue().encode()));
	        				}
	        				azResult.setAzStatusDetail(azStatusDetail);	        					
	        			} // end if balanaStatusDetail had missing attributes
	        		} // end if balanaStatusDetail had child nodes
	        	} // end if balanaStatusDetail not null	        	
	            azRspCtx.addResult(azResult);
	        	if (log.isInfoEnabled()) { 
	        		StringBuffer sbAttrs = new StringBuffer();
	        		sbAttrs = getAllAttrList(azSubj,azResource,azAction,azEnv);
	        		String subjectId = getSubjectId(azSubj);
	        		if (subjectId == null)
	        			subjectId = " (no attr w attrId = xacml:subject-id found)";
	        		log.info(
	                	"\n#############################################" + 
	            	  	"##############################################" +
	            		"\n   Finished processing request number: " + 
	            		 azRequestCounter + "-" + balanaResultCounter +
	            		"\n\tfor AzResourceActionAssociation w correlationId = " +
	            		 azRaa.getCorrelationId() +
	            		"\n\t requestor subject-id:  " + subjectId +
	            		"\n\t requested resource-id: " +
	            		 azRaa.getAzResourceActionAssociationId().getResourceId() +
	            		"\n\t requested action-id:   " +
	            		 azRaa.getAzResourceActionAssociationId().getActionId() +
	            		"\n\tDecision = " + azResult.getAzDecision() +
	            		"\n\t  All attributes: " + 
	            			sbAttrs.toString() +
	                	"\n#############################################" + 
	            	  	"##############################################\n");
	        	}
	        } // end while loop balanaResults	        
	    } // end while loop AzResourceActionAssociations        
        if (log.isTraceEnabled()){ 
        	log.trace(
    			"\n$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$" + 
    			"$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$" +
        		"\n  Completed processing AzRequestContext " +
        		"\n\twith id = " + azReqCtx.getId() +
    			"\n    Completed execution of \n\t" + 
    			this.getClass().getName() + ".decide(azReqCtx)" +
				"\n$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$" + 
				"$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$\n");
        }
        return azRspCtx;
    }
    //END of balana response prep
    
    /*
     * Returns the set of AzResourceActionAssociations that are either
     * allowed or denied based on the scope parameter and the 
     * allowedNotAllowed parameter.
     * <p>
     * Basically, what is required here is that the "scope" get resolved
     * into a set of AzResourecActionAssociation pairs and then each
     * is sent to the pdp for an individual request,
     * <p>
     * or the scope and user/environment info in the request can be
     * sent to the pdp and if policies are set up in a pre-defined
     * fashion to return scope info, then a minimal scope for the 
     * user can be returned which can then be used to query the
     * resource catalog for resource-action associations.
     * <p>
     * This class uses a test resource catalog, 
     * org.openliberty.openaz.pdp.resources.TestResourceCollection
     * 
     */
    public Set<AzResourceActionAssociation> query(
                    String scope, 
                    AzRequestContext azRequestContext,
                    boolean allowedNotAllowed){
    	
    	azRequestContext = OpenAzResourceQueryBuilder.getScopedAzRequestContext(
    			scope, azRequestContext);
    	
    	// Submit the request and process the results !?
    	AzResponseContext azResponseContext =
    		decide(azRequestContext);
    	Set<AzResourceActionAssociation> azResActAssoc = 
    			new HashSet<AzResourceActionAssociation>();
    	Set<AzResult> azResults = null;
    	if ( ! (azResponseContext == null) ) {
    		azResults = azResponseContext.getResults();
    		if ( ! ( azResults == null ) ) {
    			//azResActAssoc = 
    			//	new HashSet<AzResourceActionAssociation>();
    			for ( AzResult azResult : azResults ) {
    				// if want only permits only add permits to set
    				if (allowedNotAllowed) {
	    				if ( azResult.getAzDecision() == 
	    						AzDecision.AZ_PERMIT) {
	    					azResActAssoc.add(azResult.
	    						getAzResourceActionAssociation());	    					
	    				}
	    			// otherwise only add denies to the set
    				} else {
    					if ( azResult.getAzDecision() == 
    							AzDecision.AZ_DENY) {
    						azResActAssoc.add(azResult.
    							getAzResourceActionAssociation());
    					}
    				}
    			}
    		}
    	}
        return azResActAssoc;
    }

    public AzResponseContext queryVerbose(
            String scope, AzRequestContext azRequestContext){
    	if (log.isTraceEnabled()) log.trace(
    			"queryVerbose - returns all results");
    	azRequestContext = 
    		OpenAzResourceQueryBuilder.getScopedAzRequestContext(
    			scope, azRequestContext);
    	// Submit the request and process the results !?
    	AzResponseContext azResponseContext =
    		decide(azRequestContext);
    	return azResponseContext;
    }
    
    /*
     * Returns a StringBuffer with a formatted list of 
     * AttributeIds and Attribute values.
     */
    public StringBuffer getAllAttrList(
    		AzEntity<AzCategoryIdSubjectAccess> azSub,
    		AzEntity<AzCategoryIdResource> azRes,
    		AzEntity<AzCategoryIdAction> azAct,
    		AzEntity<AzCategoryIdEnvironment> azEnv) {
    	StringBuffer sbAttrs = new StringBuffer();

    	// Do the Subject attrs:
    	Set<AzAttribute<AzCategoryIdSubjectAccess>> subAttrs = 
			azSub.getAzAttributeSet();
		Iterator<AzAttribute<AzCategoryIdSubjectAccess>> itSubAttrs =
			subAttrs.iterator();
		sbAttrs = sbAttrs.append(
			"\n\t    Num subject attrs: " + subAttrs.size());
		while (itSubAttrs.hasNext()){
			AzAttribute<AzCategoryIdSubjectAccess> azSubAttr =
				itSubAttrs.next();
			sbAttrs = sbAttrs.append(
				"\n\t\tId: " + azSubAttr.getAttributeId() +
				"\n\t\t Val: \"" + 
				azSubAttr.getAzAttributeValue().toString() + "\"");
		}

    	// Do the Resource attrs:
    	Set<AzAttribute<AzCategoryIdResource>> resAttrs = 
			azRes.getAzAttributeSet();
		Iterator<AzAttribute<AzCategoryIdResource>> itResAttrs =
			resAttrs.iterator();
		sbAttrs = sbAttrs.append(
			"\n\t    Num resource attrs: " + resAttrs.size());
		while (itResAttrs.hasNext()){
			AzAttribute<AzCategoryIdResource> azResAttr =
				itResAttrs.next();
			sbAttrs = sbAttrs.append(
				"\n\t\tId: " + azResAttr.getAttributeId() +
				"\n\t\t Val: \"" + 
				azResAttr.getAzAttributeValue().toString() + "\"");
		}

    	// Do the Action attrs:
    	Set<AzAttribute<AzCategoryIdAction>> actAttrs = 
			azAct.getAzAttributeSet();
		Iterator<AzAttribute<AzCategoryIdAction>> itActAttrs =
			actAttrs.iterator();
		sbAttrs = sbAttrs.append(
			"\n\t    Num action attrs: " + actAttrs.size());
		while (itActAttrs.hasNext()){
			AzAttribute<AzCategoryIdAction> azActAttr =
				itActAttrs.next();
			sbAttrs = sbAttrs.append(
				"\n\t\tId: " + azActAttr.getAttributeId() +
				"\n\t\t Val: \"" + 
				azActAttr.getAzAttributeValue().toString() + "\"");
		}

    	// Do the Environment attrs:
		if (! (azEnv == null) ) {
	    	Set<AzAttribute<AzCategoryIdEnvironment>> envAttrs = 
				azEnv.getAzAttributeSet();
			Iterator<AzAttribute<AzCategoryIdEnvironment>> itEnvAttrs =
				envAttrs.iterator();
			sbAttrs = sbAttrs.append(
				"\n\t    Num environment attrs: " + envAttrs.size());
			while (itEnvAttrs.hasNext()){
				AzAttribute<AzCategoryIdEnvironment> azEnvAttr =
					itEnvAttrs.next();
				sbAttrs = sbAttrs.append(
					"\n\t\tId: " + azEnvAttr.getAttributeId() +
					"\n\t\t Val: \"" + 
					 azEnvAttr.getAzAttributeValue().toString() + "\"");
			}
		}
    	return sbAttrs;
    }
    public String getSubjectId(AzEntity<?> azSubj) {
    	String subjectId = null;
		AzAttribute<?> azSubjIdAttr = azSubj.getAttributeByAttribId(
				AzXacmlStrings.X_ATTR_SUBJECT_ID);
		if ( ! ( azSubjIdAttr == null ) ) {
			subjectId = azSubjIdAttr.getAzAttributeValue().toString();
		}
    	return subjectId;
    }
}
	  	/*
	    // START temporary test code:
	    // Note: this initial block is just test code to see if
		// we can read the test file with a request and
		// process the request. Processing the real request,
		// azReqCtx proceeds further below:
		
		// try to execute the balana test files:      	
	  	// Test code to run a decide using a test file as input
		RequestCtx request = null;
		try {
	    	request = 
	    		RequestCtx.getInstance(
	    			new FileInputStream(requestFile));
	    	request.encode(System.out, new Indenter());
	        ResponseCtx response = pdp.evaluate(request);
	        response.encode(System.out, new Indenter());
		} catch (ParsingException pe) {
			pe.printStackTrace(new PrintWriter(sw));
			System.out.println("balana ParsingException: " +
					pe.getMessage() + "\n" + sw);    		
		} catch (FileNotFoundException fnf) {
			fnf.printStackTrace(new PrintWriter(sw));
			System.out.println("balana FileNotFoundException: " +
					fnf.getMessage() + "\n" + sw);    		
		}
		// END of temporary test code, real processing begins now:
		*/

		/*
		// BEGIN TEMP TEST CODE TO OBTAIN RESULT CONTAINING OBLIGATION:
		// TEMPORARY TEST POINT: remove when obl tested
		// use the parsed "request" file from above to submit the
		// input that gives an obligation in response
		
		System.out.println("TEMPORARY OBLIGATION TEST:");
		rspCtx = pdp.evaluate(request);
		rspCtx.encode(System.out, new Indenter());
		
		// END TEMP TEST CODE TO OBTAIN RESULT CONTAINING OBLIGATION:
		*/

