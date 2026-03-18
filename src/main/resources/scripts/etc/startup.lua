-- /etc/startup.lua
-- Executed whenever the terminal boots or when you run: reboot
-- Prompt placeholders: {name}, {display}, {hostname}, {dir}

local terminalApi = term or terminal
if terminalApi ~= nil then
	if terminalApi.setAutoPrompt ~= nil then
		terminalApi.setAutoPrompt(true)
	end
	if terminalApi.setPromptTemplate ~= nil then
		terminalApi.setPromptTemplate("{name}:{dir} $ ")
	end
end

print("LuaMade Terminal v1.0")
print("Type 'help' for a list of commands")
