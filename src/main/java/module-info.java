module org.example.licentafromzero {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.licentafromzero to javafx.fxml;
    exports org.example.licentafromzero;
}