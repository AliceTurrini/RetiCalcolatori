struct Count{ int chars; int words; int lines; };
struct Directory { string nome <50>; int soglia; };

program SCANPROG {
	version SCANVERS {
		Count FILE_SCAN(string) = 1;
		int DIR_SCAN(Directory) = 2;
	} = 1;
} = 0x20000013;