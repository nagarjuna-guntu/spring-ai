//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
record Point(int x, int y) {}
record Circle(Point center, double radius) {}

void main() {
    var point = new Point(5, 8);
    var circle = new Circle(point, 2.4);
    //boundingBox(circle);
    boundingBoxSwitch(circle);
    rollOver(List.of(circle));
    Circle resized = resize(circle, 2);
}

private Circle resize(Circle circle, int factor) {
    Circle(Point center, double radius) = circle;
    assert center != null;
    Point(int x, int y) = center;
    return new Circle(new Point(x + factor, y + factor), radius + factor);
}

private void rollOver(List<Circle> circles) {
    for (Circle(Point(int x, int y), double radius) : circles) {
        //can use x, y, and radius
    }
}

void boundingBox(Circle c) {
    if (c instanceof Circle(Point(var x, var y), var radius)) {
        int minX = (int) Math.floor(x - radius); int maxX = (int) Math.ceil(x + radius);
        int minY = (int) Math.floor(y - radius); int maxY = (int) Math.ceil(y + radius);
        IO.println("minX :: " + minX + " maxX :: " + maxX );
        IO.println("minY :: " + minY + " maxX :: " + maxY );
    }
}

void boundingBoxSwitch(Circle c) {
    switch (c) {
        case Circle(Point(var x, var y), var radius) -> {
            int minX = (int) Math.floor(x - radius); int maxX = (int) Math.ceil(x + radius);
            int minY = (int) Math.floor(y - radius); int maxY = (int) Math.ceil(y + radius);
            IO.println("minX :: " + minX + " maxX :: " + maxX );
            IO.println("minY :: " + minY + " maxX :: " + maxY );
        }
    }

}

void boundingBoxEnhaceLocalVarDeclaration(Circle c) {

        Circle(Point(var x, var y), var radius)  = c;
            int minX = (int) Math.floor(x - radius); int maxX = (int) Math.ceil(x + radius);
            int minY = (int) Math.floor(y - radius); int maxY = (int) Math.ceil(y + radius);
            IO.println("minX :: " + minX + " maxX :: " + maxX );
            IO.println("minY :: " + minY + " maxX :: " + maxY );

    }