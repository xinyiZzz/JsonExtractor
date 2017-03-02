import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author XinYi
 * @version 1.1.0
 * @since 创建时间：2016年12月10日 
 * name： JSON数据抽取
 * 
 * 功能描述： 给定的JSON格式字符串和抽取规则，抽取其指定key下的value
 * 	包含三种匹配模式：完整匹配、精确匹配、模糊匹配
 * 		(注：1、精确匹配、模糊匹配在内部均转化为完整匹配进行抽取
 * 			2、规则中 # 号前面代表key，后面代表其所在当前层级，两层key规则之间用 - 连接)
 * 		下面均以例子< 在{A:{B:[C1:"X", C2:"Y"], D:"", C3:""}, E:………}中匹配 X和Y，其中二者父层C1和C2同名 >来说明匹配模式
 * 		完整匹配：给定待匹配数据和目标key及其对应括号层级的匹配规则(即所在的括号层数，包括中括号[]和大括号{})，返回匹配结果列表。
 * 			上述C1和C2当前处于第三层括号中，所以可用的匹配规则有：A#1-B#1-C#1 , C#3 , A#1-C#2 , B#2-C#1 , A#1-C#0
 *		精确匹配：给定待匹配数据和目标key对应的精确规则，返回匹配结果列表。
 *			针对上例，精确规则为：A-B-C，内部自动转化为精确完整规则：A#1-B#1-C#1 进行匹配，所以实际是对精确完整规则的简化
 *		模糊匹配：给定待匹配数据和目标key对应的模糊规则，返回匹配结果列表。
 *			针对上例，模糊规则为：A-C，内部自动转化为模糊完整规则：A#0-C#0 进行匹配，不推荐使用，无法准确抽取
 * 		(注：
 * 			1、规则中下一个key的层级是在上一个key的value中对应的层级，即B处于整体第二层级时，单独抽取用规则 B#2，
 * 				先抽取A时规则为 A#1-B#1，即第一步抽取完A后，当前B处于A的value中第一层；
 * 			2、上述 A#1-B#1-C#1 为精确完整规则，即指定每一括号层级的key；
 * 			3、上述 A#1-C#0 为模糊完整规则，当key的层级指定0时表示不限制层级，将匹配当前所有层级下的key，
 * 				精确匹配到A后模糊匹配C，即当匹配到A后，在A中不限制层级匹配C，注意，当规则为 C#0 时，将匹配到C1、C2、C3，不符合要求；
 * 				当目标要匹配C3时，则仅用模糊规则无法准确匹配，模糊规则A#0-C#0将匹配到C1、C2、C3
 * 			4、上述 其余规则为普通规则，即指定部分括号层级的key；
 * 			3、当规则为 C#3 时仅匹配C1和C2，因为C3处于整体的第二层，所以不匹配
 * 			4、当规则为 A#1-C#2 时仅匹配C1和C2，因为第一步匹配整体第一层的A后，C3处于当前第一层
 * 			5、之所以返回匹配结果列表，是因为当父层有列表结构时，json同一层级下可能包含同名的key，如上述情况
 * 				目标key为第三层的C1和C2，因为B对应的value是个列表，所以C1和C2同名且在同一层级下
 * 			6、返回结果有两种格式，1、指定key对应的所有key:value完整数据，2、指定key对应的value(当key对应最后一层时)
 * 				例如当规则为 B#2 时返回 B:[C1:"", C2:""]; 当规则为 B#2-C#1 时返回 X和Y
 * 			7、列表特例：当为{A:{B:[{C1:""}, {C2:""}], D:""}, E:………}时，完整规则为A#1-B#1-C#2，虽然B下一个key是C，但由于其间有中括号
 * 				所以C位于B下两层，也可用精确规则：A-B-[]-C，模糊规则：A-B-C
 * 			8、利用栈检测json格式的字符串中括号的匹配情况来进行指定层级key的识别，目前发现的所有不标准JSON格式均可正确抽取，包括括号缺失、JSON内部出错等
 * 			
 * 			***9、推荐仅使用 完整匹配 和 精确匹配，不推荐模糊匹配。精确匹配仅为了能准确定位的时候简化规则，模糊匹配为了兼容之前收集的规则和测试的时候节约时间
 * 		)
 */

public class DataPretreat {

	/**
	 * Description: 获取正则对象
	 * 
	 * @param 待匹配数据
	 * @param 正则表达式
	 * @return 正则对象
	 * @Note Author: lvfang
	 */
	public static Matcher StrPatternCI(String data, String pattern) {
		Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(data); // 获取 matcher 对象
		return m;
	}
	
	/**
	 * Description: 给定待匹配数据和正则表达式，返回所有匹配结果字符串的开始位置和终止位置
	 * 
	 * @param 待匹配数据
	 * @param 正则表达式
	 * @return 匹配结果位置列表，偶数为开始位置，奇数为终止位置，从头前后对应
	 * @Note Author: Xinyi, Date: 2016年12月16日 下午5:24:42
	 */
	public static ArrayList<Integer> key_pattern_seek(String message_data,
			String pattern) {
		Matcher m = StrPatternCI(message_data, pattern);
		ArrayList<Integer> key_positions = new ArrayList<Integer>();
		while (m.find()) {
			key_positions.add(m.start());
			key_positions.add(m.end());
		}
		return key_positions;
	}

	

	/**
	 * Description: 利用栈检测json格式的字符串中括号的匹配情况 
	 * 注：1、first_empty_position 可用于头部完整json子串的抽取；
	 * 		2、left_residue_brackets 可用于json括号补全
	 * 		3、right_residue_brackets 可用于获得指定key处于第几层，
	 * 		4、当json字符串内部出错时，例如：{{)}} 的情况，返回结果为 {right_error_brackets=}, flag=false, left_residue_brackets={,
	 * 			first_empty_position=0, left_error_brackets=(, right_residue_brackets=}，可根据结果推测出上述出错情况，当无法得到出错位置
	 * 		5、应用：由于JSON中仅包含中括号[]和大括号{}，所以忽略小括号()，且部分出错json格式外围有一层小括号()，此时可正确处理
	 * 
	 * @param 待检测json字符串
	 * @return {栈第一次为空时的位置，多余的左括号串，多余的右括号串，匹配出错的左括号串, 匹配出错的右括号串}
	 * @Note Author: Xinyi, Date: 2016年12月14日 下午8:14:50
	 */
	public static Map<String, String> json_brackets_check(String str) {
		Stack<Character> stack = new Stack<Character>();
		Map<String, String> brackets_check_map = new HashMap<String, String>();
		boolean flag = true;
		int first_empty_position = 0; // 有两种情况，1:当第一次匹配到的括号是右括号时的位置，2：当第一次匹配到的括号是左括号并压栈后栈第一次为空的位置，当json文件头部缺失时，用于截取掉尾部与缺失的头部对应的部分；对应程序中用于得到指定key的完整value
		String left_residue_brackets = ""; // 多余的左括号串，即遍历结束时，栈中剩余的左括号
		String right_residue_brackets = ""; // 多余的右括号串，即栈为空时遍历到右括号；对应程序中用于当json文件头部缺失时，用于获得指定key处于第几层
		String left_error_brackets = ""; // 匹配出错的左括号串，即左右括号类型不同，此时弹出左括号
		String right_error_brackets = ""; // 匹配出错的右括号串，同上，与left_error_brackets中的出错左括号一一对应
		char left_bracket;
		for (int i = 0; i < str.length(); i++) {
			try {
				switch (str.charAt(i)) {
//				case '(':
				case '[':
				case '{':
					stack.push(str.charAt(i));
					break;
//				case ')':
//					left_bracket = stack.pop();
//					if (left_bracket != '(') {
//						left_error_brackets += left_bracket;
//						right_error_brackets += ")";
//						flag = false;
//					}
//					if (stack.isEmpty())
//						if (first_empty_position == 0)
//							first_empty_position = i + 1;
//					break;
				case ']':
					left_bracket = stack.pop();
					if (left_bracket != '[') {
						left_error_brackets += left_bracket;
						right_error_brackets += "]";
						flag = false;
					}
					if (stack.isEmpty())
						if (first_empty_position == 0)
							first_empty_position = i + 1;
					break;
				case '}':
					left_bracket = stack.pop();
					if (left_bracket != '{') {
						left_error_brackets += left_bracket;
						right_error_brackets += "}";
						flag = false;
					}
					if (stack.isEmpty())
						if (first_empty_position == 0)
							first_empty_position = i + 1; // 保留右括号，说明目标key的value包含括号，如：A:{B}
					break;
				}
			} catch (Exception e) {
				if (stack.isEmpty())
					if (first_empty_position == 0)
						first_empty_position = i; // 此时右括号不保留，如：A:"B"}，此时右括号属于父key
				right_residue_brackets += str.charAt(i);
				flag = false;
			}
		}
		while (!stack.isEmpty()) {
			left_residue_brackets += stack.pop();
		}
		if (flag && !stack.isEmpty())
			flag = false;
		brackets_check_map.put("flag", Boolean.toString(flag));
		brackets_check_map.put("first_empty_position",
				String.valueOf(first_empty_position));
		brackets_check_map.put("left_residue_brackets", left_residue_brackets);
		brackets_check_map
				.put("right_residue_brackets", right_residue_brackets);
		brackets_check_map.put("left_error_brackets", left_error_brackets);
		brackets_check_map.put("right_error_brackets", right_error_brackets);
		return brackets_check_map;
	}

	/**
	* Description: 给定待匹配数据和key，返回所有数据中指定key的位置开始，到待匹配数据最后位置结束的字符串，当指定key出现多次时，对应多个匹配结果字符串
	* 
	* @param 待匹配数据
	* @param key
	* @return 指定key的位置到最后的子串列表
	* @Note Author: Xinyi, Date: 2016年12月16日 下午5:30:06
	*/
	public static ArrayList<String> key_position_seek(String message_data, String key) {
		String pattern = "\"" + key + "\": *";
		ArrayList<String> key_end_strlist = new ArrayList<String>(); // 
		int start_position = -1;
		int end_position = message_data.length();
		for (int position : key_pattern_seek(message_data, pattern)) {
			if (start_position == -1) {
				start_position = position;
			} else {
				key_end_strlist.add(message_data.substring(start_position,
						end_position));
				start_position = -1;
			}
		}
		// for(String tmp : key_end_strlist){"key_end_strlist"+System.out.println(tmp);}
		return key_end_strlist;
	}

	/**
	* Description: 给定待匹配数据、key和key所在层级(即所在的括号层数，0时表示不限制层级), 返回匹配结果
	* 注：返回数据为列表，因为当父层有列表结构时，json同一层级下可能包含同名的key，如message_data是{A:{B:[C1:"X", C2:"Y"], D:"", C3:""}, E:………}，
	* 		目标key为第三层的C1和C2，因为B对应的value是个列表，所以C1和C2同名且在同一层级下
	* 
	* @param 待匹配数据
	* @param key
	* @param key所在层级，为0时表示不限制层级
	* @return 匹配结果列表，有两种格式，1、指定key对应的所有key:value完整数据，2、指定key对应的value(当key对应最后一层时)
 * 			例如{A:{B:[C1:"X", C2:"Y"], D:"", C3:""}, E:………}, 当规则为 B#2 时返回 B:[C1:"", C2:""]，当规则为 B#2-C#1 时返回 X和Y
	* @Note Author: Xinyi, Date: 2016年12月16日 下午5:39:54
	*/
	public static ArrayList<String> key_level_match(String message_data, String key, int level) {
		ArrayList<String> key_end_strlist = key_position_seek(message_data, key);
		ArrayList<String> match_key_value_list = new ArrayList<String>();
		for (String key_end_str : key_end_strlist) { //当找到一个指定层级下的目标key时不停止，处理父层有列表结构的情况
			Map<String, String> brackets_check_map = json_brackets_check(key_end_str);
			if (brackets_check_map.get("right_residue_brackets").length() == level || level == 0) { // 说明是指定层级下的key
				String str_value_pattern = "\"" + key + "\":\\s*\"(.*?)\"";
				Matcher str_value_m = StrPatternCI(key_end_str, str_value_pattern);
				String figure_value_pattern = "\"" + key + "\":\\s*(\\d+)";
				Matcher figure_value_m = StrPatternCI(key_end_str, figure_value_pattern);
				String bool_true_value_pattern = "\"" + key + "\":\\s*(true)";
				Matcher bool_true_value_m = StrPatternCI(key_end_str, bool_true_value_pattern);
				String bool_false_value_pattern = "\"" + key + "\":\\s*(false)";
				Matcher bool_false_value_m = StrPatternCI(key_end_str, bool_false_value_pattern);
				if (str_value_m.lookingAt()) { // 说明目标key已经到最后一层，即为A:"B"结构，直接匹配返回
					match_key_value_list.add(str_value_m.group(1));
				}
				else if (figure_value_m.lookingAt()) { // 说明目标key已经到最后一层，即为A:"B"结构，直接匹配返回
					match_key_value_list.add(figure_value_m.group(1));
				}
				else if (bool_true_value_m.lookingAt()) { // 说明目标key已经到最后一层，即为A:"B"结构，直接匹配返回
					match_key_value_list.add(bool_true_value_m.group(1));
				}
				else if (bool_false_value_m.lookingAt()) { // 说明目标key已经到最后一层，即为A:"B"结构，直接匹配返回
					match_key_value_list.add(bool_false_value_m.group(1));
				}
				else { // 说明目标key为A:{B}或A:[B]结构，通过括号检测的结果，返回key对应的完整括号中内容
//					System.out.println("key_value_match  "+key_end_str);
					match_key_value_list.add(key_end_str.substring(0, Integer.valueOf(brackets_check_map.get("first_empty_position")).intValue()));
				}
			}
		}
//		for (String tmp : match_key_value_list) {System.out.println("match_key_value_list" + tmp);}
		return match_key_value_list;
	}
	
	/**
	* Description: 数据预处理，目前包括替换数据中的 \" 字符(数据中包含html时会出现，对匹配A:"B"中B时产生影响)、尾部缺失括号补全
	* 
	* @param 原始数据
	* @return 处理后数据
	* @Note Author: Xinyi, Date: 2016年12月16日 下午10:29:01
	*/
	public static String message_data_pretreat(String message_data) {
		message_data = message_data.replaceAll("\\\\\"", "zhe_li_you_yi_ge_yin_hao");
		Map<String, String> brackets_check_map = json_brackets_check(message_data);
		String left_residue_brackets = brackets_check_map.get("left_residue_brackets");
		for(int i = 0; i < left_residue_brackets.length(); i++) {
			if(left_residue_brackets.charAt(i) == '{'){
				message_data += "}";
			} else if(left_residue_brackets.charAt(i) == '['){
				message_data += "]";
			}
        } 
		return message_data;
	}
	
	/**
	* Description: 数据抽取完成后处理，目前还原数据中的 \" 字符
	* @param 抽取后的数据
	* @return 处理后数据
	* @Note Author: Xinyi, Date: 2016年12月16日 下午10:32:05
	*/
	public static String message_data_last_pretreat(String message_data) {
		message_data = message_data.replaceAll("zhe_li_you_yi_ge_yin_hao", "\\\\\"");
		return message_data;
	}
	
	/**
	* Description: 给定待匹配数据和目标key及其对应层级的匹配规则，返回匹配结果列表
	* 	1、规则中 # 号前面代表key，后面代表其所在当前层级，两层key规则之间用 - 连接
	* 	2、下面以例子< 在{A:{B:[C1:"X", C2:"Y"], D:"", C3:""}, E:………}中匹配 X和Y，其中二者父层C1和C2同名 >来说明匹配模式
	* 		上述C1和C2当前处于第三层括号中，所以可用的匹配规则有：A#1-B#1-C#1 , C#3 , A#1-C#2 , B#2-C#1 , A#1-C#0
	* 	(注：
	* 		1、规则中下一个key的层级是在上一个key的value中对应的层级，即B处于整体第二层级时，单独抽取用规则 B#2，
	* 			先抽取A时规则为 A#1-B#1，即第一步抽取完A后，当前B处于A的value中第一层；
	* 		2、上述 A#1-B#1-C#1 为精确完整规则，即指定每一括号层级的key；
 	* 		3、上述 A#1-C#0 为模糊完整规则，当key的层级指定0时表示不限制层级，将匹配当前所有层级下的key，
 * 				精确匹配到A后模糊匹配C，即当匹配到A后，在A中不限制层级匹配C，注意，当规则为 C#0 时，将匹配到C1、C2、C3，不符合要求；
 * 				当目标要匹配C3时，则仅用模糊规则无法准确匹配，模糊规则A#0-C#0将匹配到C1、C2、C3
 	* 		4、上述 其余规则为普通规则，即指定部分括号层级的key；
 	* 		3、当规则为 C#3 时仅匹配C1和C2，因为C3处于整体的第二层，所以不匹配
 	* 		4、当规则为 A#1-C#2 时仅匹配C1和C2，因为第一步匹配整体第一层的A后，C3处于当前第一层
 	* 		5、列表特例：当为{A:{B:[{C1:""}, {C2:""}], D:""}, E:………}时，完整规则为A#1-B#1-C#2，虽然B下一个key是C，但由于其间有中括号
 	* 			所以C位于B下两层，也可用精确规则：A-B-[]-C，模糊规则：A-B-C
 	* 		)
	* 
	* @param 待匹配数据
	* @param 目标key及其对应层级的匹配规则，格式如上“注”中所示
	* @return 匹配结果列表
	* @Note Author: Xinyi, Date: 2016年12月16日 下午7:25:35
	*/
	public static ArrayList<String> key_level_rule_match(String message_data, String key_level_rule) {
		message_data = message_data_pretreat(message_data);
		String[] key_level_rule_list = key_level_rule.split("-");
		ArrayList<String> start_key_value_list = new ArrayList<String>();
		start_key_value_list.add(message_data);
		ArrayList<String> matched_key_value_list = new ArrayList<String>();
		int accumulate_middle_brackets = 0; // 累积的中括号数量，类似 A-B-[]-C 的规则中用到
        for (int i = 0; i < key_level_rule_list.length; i++) {
        	matched_key_value_list = new ArrayList<String>();
        	String match_key = key_level_rule_list[i].split("#")[0];
        	int match_level = Integer.valueOf(key_level_rule_list[i].split("#")[1]).intValue();
        	if(match_key.equals("[]")){
        		accumulate_middle_brackets += match_level;
        		continue;
        	} else if(accumulate_middle_brackets != 0){
        		match_level += accumulate_middle_brackets;
        		accumulate_middle_brackets = 0;
        	}
        	for(String key_value : start_key_value_list){
        		for(String key_value_match : key_level_match(key_value, match_key, match_level)){
//        			System.out.println("key_value_match  "+key_value_match);
        			matched_key_value_list.add(key_value_match);
        		}
        	}
        	start_key_value_list = matched_key_value_list;
//        	System.out.println("matched_key_value_list"+matched_key_value_list);
        }
        ArrayList<String> final_matched_key_value_list = new ArrayList<String>();
        for(String matched_key_value : matched_key_value_list){
        	final_matched_key_value_list.add(message_data_last_pretreat(matched_key_value));
        }
		return final_matched_key_value_list;
	}
	
	/**
	* Description: 给定目标key对应的不完整规则和key的对应的层级，返回完整规则，注：目前需求只用统一指定层级
	* 
	* @param 目标key对应的不完整规则(即A-B-C的形式)
	* @param key的对应的层级
	* @return 完整规则(即例如A#1-B#1-C#1的形式)
	* @Note Author: Xinyi, Date: 2016年12月16日 下午7:25:35
	*/
	public static String match_rule_transitive(String match_rule, int level) {
		String[] match_rule_list = match_rule.split("-");
		String complete_rule_rule = "";
		for (int i = 0; i < match_rule_list.length; i++) {
			if(!complete_rule_rule.equals("")){
				complete_rule_rule += "-";
			}
			complete_rule_rule += (match_rule_list[i] + "#" + level);
		}
		return complete_rule_rule;
	}
	
	/**
	* Description: 给定待匹配数据和目标key对应的完整规则(可指定部分key的层级)，返回匹配结果列表
	* 
	* @param 待匹配数据
	* @param 目标key对应的完整规则，例如：{A:{B:[C1:"X", C2:"Y"], D:"", C3:""}, E:………}，匹配X和Y，
	* 	可用的匹配规则有：A#1-B#1-C#1 , C#3 , A#1-C#2 , B#2-C#1 , A#1-C#0
	* @return 匹配结果列表
	* @Note Author: Xinyi, Date: 2016年12月16日 下午7:27:35
	*/
	public static ArrayList<String> complete_rule_match(String message_data, String complete_rule) {
		return key_level_rule_match(message_data, complete_rule);
	}
	
	/**
	* Description: 给定待匹配数据和目标key对应的精确规则，返回匹配结果列表
	* 
	* @param 待匹配数据
	* @param 目标key对应的精确规则，例如：{A:{B:[C1:"X", C2:"Y"], D:"", C3:""}, E:………}，匹配X和Y，规则为：A-B-C，
	* 	内部自动转化为精确完整规则：A#1-B#1-C#1 进行匹配，所以实际是对精确完整规则的简化
	* @return 匹配结果列表
	* @Note Author: Xinyi, Date: 2016年12月16日 下午7:25:35
	*/
	public static ArrayList<String> accurate_rule_match(String message_data, String accurate_rule) {
		return key_level_rule_match(message_data, match_rule_transitive(accurate_rule, 1));
	}
	
	/**
	* Description: 给定待匹配数据和目标key对应的模糊规则，返回匹配结果列表
	* 
	* @param 待匹配数据
	* @param 目标key对应的模糊规则，例如：{A:{B:[C1:"X", C2:"Y"], D:"", C3:""}, E:………}，匹配X和Y，规则为：A-C，
	* 	内部自动转化为模糊完整规则：A#0-C#0 进行匹配，不推荐使用，无法准确抽取
	* @return 匹配结果列表
	* @Note Author: Xinyi, Date: 2016年12月16日 下午7:25:35
	*/
	public static ArrayList<String> blur_rule_match(String message_data, String blur_rule) {
		return key_level_rule_match(message_data, match_rule_transitive(blur_rule, 0));
	}

	public static void main(String[] args) {
		DataPretreat dp = new DataPretreat();
		String original_data = encode_test
				.readAbsFileToStringUtf8("/Users/zouxinyi/test");
		System.out.println(original_data.length());
		//System.out.println(dp.key_level_match(original_data, "title", 1));
		System.out.println(dp.complete_rule_match(original_data, "gallery#1-abstract#2"));
	}
}
