modify = "public-f "
with open("src\\main\\resources\\META-INF\\accesstransformer.cfg", mode="a+") as f:
    while True:
        clazz = input("at:")
        f.write(modify + clazz + "\n" + modify + clazz + " *\n" + modify + clazz + " *()\n")
        f.flush()
    