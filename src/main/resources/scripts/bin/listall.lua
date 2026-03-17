-- Recursive file lister
-- Usage: run /bin/listall.lua [directory]

local function listRecursive(dir, indent)
    indent = indent or 0
    local files = fs.list(dir)
    for i = 1, #files do
        local file = files[i]
        local path = dir .. "/" .. file
        local prefix = string.rep("  ", indent)
        if fs.isDir(path) then
            print(prefix .. file .. "/")
            listRecursive(path, indent + 1)
        else
            print(prefix .. file)
        end
    end
end

local dir = args[1] or "/"
print("Listing: " .. dir)
listRecursive(dir)
