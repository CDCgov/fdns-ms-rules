package gov.cdc.foundation.controller;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.cdc.foundation.helper.LoggerHelper;
import gov.cdc.foundation.helper.MessageHelper;
import gov.cdc.helper.ErrorHandler;
import gov.cdc.helper.ObjectHelper;
import gov.cdc.helper.common.ServiceException;
import gov.cdc.engine.SimpleValidator;
import gov.cdc.engine.ValidatorException;
import gov.cdc.engine.result.CompoundValidationResult;
import gov.cdc.engine.result.ValidationResult;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import springfox.documentation.annotations.ApiIgnore;

@Controller
@EnableAutoConfiguration
@RequestMapping("/api/1.0/")
public class RulesController {

	private static final Logger logger = Logger.getLogger(RulesController.class);
	
	@Value("${version}")
	private String version;

	private String profileRegex;

	public RulesController(@Value("${profile.regex}") String profileRegex) {
		this.profileRegex = profileRegex;
	}

	@RequestMapping(method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<?> index() throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		
		Map<String, Object> log = new HashMap<>();
		
		try {
			JSONObject json = new JSONObject();
			json.put("version", version);
			return new ResponseEntity<>(mapper.readTree(json.toString()), HttpStatus.OK);
		} catch (Exception e) {
			logger.error(e);
			LoggerHelper.getInstance().log(MessageHelper.METHOD_INDEX, log);
			
			return ErrorHandler.getInstance().handle(e, log);
		}
	}


	@PreAuthorize("!@authz.isSecured() or #oauth2.hasScope('rules:'.concat(#profile))")
	@RequestMapping(value = "{profile}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Create or update rules for the specified profile", notes = "Create or update rules")
	@ResponseBody
	public ResponseEntity<?> upsertRulesWithPut(
			@ApiIgnore @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@RequestBody(required = true) String payload, 
			@ApiParam(value = "Profile identifier") @PathVariable(value = "profile") String profile) {
		return upsertRules(authorizationHeader, payload, profile);
	}

	@PreAuthorize("!@authz.isSecured() or #oauth2.hasScope('rules:'.concat(#profile))")
	@RequestMapping(value = "{profile}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Create or update rules for the specified profile", notes = "Create or update rules")
	@ResponseBody
	public ResponseEntity<?> upsertRules(
			@ApiIgnore @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@RequestBody(required = true) String payload, 
			@ApiParam(value = "Profile identifier") @PathVariable(value = "profile") String profile) {

		ObjectMapper mapper = new ObjectMapper();

		Map<String, Object> log = new HashMap<>();
		log.put(MessageHelper.CONST_METHOD, MessageHelper.METHOD_UPSERTRULES);

		try {

			// First, check the profile ID
			Pattern p = Pattern.compile(profileRegex);
			if (!p.matcher(profile).matches())
				throw new ServiceException(String.format(MessageHelper.ERROR_PROFILE_IDENTIFIER_INVALID, profileRegex));

			JSONObject data = new JSONObject(payload);
			ObjectHelper helper = ObjectHelper.getInstance(authorizationHeader);
			if (helper.exists(profile))
				helper.updateObject(profile, data);
			else
				helper.createObject(data, profile);

			JSONObject json = new JSONObject();
			json.put(MessageHelper.CONST_SUCCESS, true);
			json.put(MessageHelper.CONST_PROFILE, profile);
			return new ResponseEntity<>(mapper.readTree(json.toString()), HttpStatus.OK);

		} catch (Exception e) {
			logger.error(e);
			LoggerHelper.getInstance().log(MessageHelper.METHOD_UPSERTRULES, log);
			
			return ErrorHandler.getInstance().handle(e, log);
		}

	}

	@PreAuthorize("!@authz.isSecured() or #oauth2.hasScope('rules:'.concat(#profile))")
	@RequestMapping(value = "{profile}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Get saved rules for the specified profile", notes = "Get saved rules")
	@ResponseBody
	public ResponseEntity<?> getRules(
			@ApiIgnore @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@ApiParam(value = "Profile identifier") @PathVariable(value = "profile") String profile) {

		ObjectMapper mapper = new ObjectMapper();

		Map<String, Object> log = new HashMap<>();
		log.put(MessageHelper.CONST_METHOD, MessageHelper.METHOD_GETRULES);

		try {
			String objectId = profile;

			ObjectHelper helper = ObjectHelper.getInstance(authorizationHeader);
			if (!helper.exists(objectId))
				throw new ServiceException(MessageHelper.ERROR_PROFILE_DOESNT_EXIST);

			return new ResponseEntity<>(mapper.readTree(helper.getObject(objectId).toString()), HttpStatus.OK);

		} catch (Exception e) {
			logger.error(e);
			LoggerHelper.getInstance().log(MessageHelper.METHOD_GETRULES, log);
			
			return ErrorHandler.getInstance().handle(e, log);
		}

	}

	@PreAuthorize("!@authz.isSecured() or #oauth2.hasScope('rules:'.concat(#profile))")
	@RequestMapping(value = "validate/{profile}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Validate JSON message", notes = "Valides a JSON object with a stored configuration.")
	@ResponseBody
	public ResponseEntity<?> validate(
			@ApiIgnore @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@RequestBody(required = true) String payload, 
			@ApiParam(value = "Profile identifier") @PathVariable(value = "profile") String profile, 
			@RequestParam(defaultValue = "false") boolean explain) {
		ObjectMapper mapper = new ObjectMapper();

		Map<String, Object> log = new HashMap<>();
		log.put(MessageHelper.CONST_METHOD, MessageHelper.METHOD_VALIDATE);

		try {
			// Get the json object
			JSONObject json = new JSONObject(payload);
			JSONArray explainationDetails = new JSONArray();
			JSONObject responseObj = new JSONObject();

			// Get the rules config
			JSONObject rules = null;
			try {
				rules = ObjectHelper.getInstance(authorizationHeader).getObject(profile);
				rules.remove("_id");
			} catch (ResourceAccessException e) {
				throw new ServiceException(e);
			} catch (Exception e) {
				logger.error(e);
			}

			// Let's check validate the message
			int nbOfErrors = checkValidationRules(json, rules, explain, explainationDetails);

			responseObj.put(MessageHelper.CONST_VALID, nbOfErrors == 0);
			responseObj.put("errors", nbOfErrors);

			if (explain) {
				responseObj.put("details", explainationDetails);
			}

			log.put(MessageHelper.CONST_SUCCESS, true);
			log.put(MessageHelper.CONST_VALID, nbOfErrors == 0);
			log.put("errors", nbOfErrors);
			LoggerHelper.getInstance().log(MessageHelper.METHOD_VALIDATE, log);

			return new ResponseEntity<>(mapper.readTree(responseObj.toString()), HttpStatus.OK);
		} catch (Exception e) {
			logger.error(e);
			LoggerHelper.getInstance().log(MessageHelper.METHOD_VALIDATE, log);

			return ErrorHandler.getInstance().handle(e, log);
		}
	}

	@RequestMapping(value = "validate", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Validate JSON message", notes = "Valides a JSON object with a provided configuration.")
	@ResponseBody
	public ResponseEntity<?> validate(@ApiParam(value = "JSON file to validate") @RequestParam("json") MultipartFile json, @ApiParam(value = "Rules configuration") @RequestParam("rules") MultipartFile rules, @RequestParam(defaultValue = "false") boolean explain) {
		ObjectMapper mapper = new ObjectMapper();

		Map<String, Object> log = new HashMap<>();
		log.put(MessageHelper.CONST_METHOD, MessageHelper.METHOD_VALIDATE);

		try {
			// Get the json version
			JSONObject jsonObj = new JSONObject(IOUtils.toString(json.getInputStream(), Charset.defaultCharset()));
			JSONArray explainationDetails = new JSONArray();
			JSONObject responseObj = new JSONObject();

			// Get the rules configuration
			JSONObject rulesObj = new JSONObject(IOUtils.toString(rules.getInputStream(), Charset.defaultCharset()));

			// Let's check validate the message
			int nbOfErrors = checkValidationRules(jsonObj, rulesObj, explain, explainationDetails);

			responseObj.put(MessageHelper.CONST_VALID, nbOfErrors == 0);
			responseObj.put("errors", nbOfErrors);

			if (explain) {
				responseObj.put("details", explainationDetails);
			}

			log.put(MessageHelper.CONST_SUCCESS, true);
			log.put(MessageHelper.CONST_VALID, nbOfErrors == 0);
			log.put("errors", nbOfErrors);
			LoggerHelper.getInstance().log(MessageHelper.METHOD_VALIDATE, log);

			return new ResponseEntity<>(mapper.readTree(responseObj.toString()), HttpStatus.OK);
		} catch (Exception e) {
			logger.error(e);
			LoggerHelper.getInstance().log(MessageHelper.METHOD_VALIDATE, log);

			return ErrorHandler.getInstance().handle(e, log);
		}
	}

	private int checkValidationRules(JSONObject payload, JSONObject rules, boolean explain, JSONArray explainationDetails) throws ServiceException, ValidatorException {
		int nbOfInvalidItems = 0;

		List<ValidationResult> vrList = executeRules(payload, rules);
		if (vrList != null)
			for (ValidationResult vrItem : vrList) {
				if (!vrItem.isValid())
					nbOfInvalidItems++;
				if (explain)
					explainationDetails.put(createExplanationDetail(vrItem));
			}

		return nbOfInvalidItems;
	}

	private JSONObject createExplanationDetail(ValidationResult vr) {
		JSONObject detail = new JSONObject();
		detail.put("ID", vr.getDescription());
		detail.put("title", vr.getComment());
		detail.put("command", vr.getCommand());
		if (vr.getRule() != null)
			detail.put("rule", new JSONObject(vr.getRule()));
		detail.put("passed", vr.isValid());
		return detail;
	}

	private List<ValidationResult> executeRules(JSONObject payload, JSONObject rules) throws ServiceException, ValidatorException {
		List<ValidationResult> checkList = null;
		if (rules != null) {
			// Then, check the JSON Object
			ValidationResult vr = applyRules(payload, rules);

			if (vr instanceof CompoundValidationResult) {
				CompoundValidationResult cvr = (CompoundValidationResult) vr;
				checkList = cvr.flatten();
			}

		}
		return checkList;
	}

	public ValidationResult applyRules(JSONObject object, JSONObject rules) throws ServiceException {
		try {
			SimpleValidator v = new SimpleValidator();
			v.initialize(rules);
			return v.validate(object);
		} catch (ValidatorException e) {
			throw new ServiceException(e);
		}
	}

}