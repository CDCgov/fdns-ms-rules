package gov.cdc.foundation.helper;

import gov.cdc.helper.AbstractMessageHelper;

public class MessageHelper extends AbstractMessageHelper {

	public static final String CONST_WARNING = "warning";
	public static final String CONST_PROFILE = "profile";
	public static final String CONST_VALID = "valid";

	public static final String METHOD_INDEX = "index";
	public static final String METHOD_UPSERTRULES = "upsertRules";
	public static final String METHOD_GETRULES = "getRules";
	public static final String METHOD_VALIDATE = "validate";

	public static final String ERROR_PROFILE_IDENTIFIER_INVALID = "The profile identifier is not valid, it must match the following expression: %s";
	public static final String ERROR_PROFILE_DOESNT_EXIST = "This profile doesn't exist.";

	private MessageHelper() {
		throw new IllegalAccessError("Helper class");
	}

}
